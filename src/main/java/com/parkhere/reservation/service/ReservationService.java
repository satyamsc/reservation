package com.parkhere.reservation.service;

import com.parkhere.reservation.exception.ReservationServiceException;
import com.parkhere.reservation.model.ReservationRequest;
import com.parkhere.reservation.model.ReservationResponse;
import com.parkhere.reservation.util.DBConstants;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;
import software.amazon.awssdk.services.eventbridge.EventBridgeClient;
import software.amazon.awssdk.services.eventbridge.model.PutEventsRequest;
import software.amazon.awssdk.services.eventbridge.model.PutEventsRequestEntry;

import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

@Service
@AllArgsConstructor
@Slf4j
public class ReservationService {


    private final DynamoDbClient dynamoDbClient;
    private final EventBridgeClient eventBridgeClient;

    private final SpotAllocationService spotAllocationService;

    public ReservationResponse createReservation(int parkingLotId, ReservationRequest request) {
        validateRequest(request);

        if (userHasOverlappingReservations(request)) {
            throw new ReservationServiceException("User has overlapping reservations");
        }

        Map<String, Object> allocationResult = spotAllocationService.allocateSpot(parkingLotId, request.getStartTimestamp(), request.getEndTimestamp());
        if (allocationResult == null || !allocationResult.containsKey(DBConstants.SPOT_ID)) {
            return null;
        }

        int spotId = (int) allocationResult.get(DBConstants.SPOT_ID);
        int reservationId = ThreadLocalRandom.current().nextInt(Integer.MAX_VALUE);

        createReservationInDynamoDB(parkingLotId, request, spotId, reservationId);
        publishReservationEvent(reservationId);

        return new ReservationResponse(reservationId, spotId, request.getStartTimestamp(), request.getEndTimestamp());
    }

    private void validateRequest(ReservationRequest request) {
        if (request.getStartTimestamp() >= request.getEndTimestamp()) {
            throw new ReservationServiceException("startTimestamp must be less than endTimestamp");
        }
    }

    private boolean userHasOverlappingReservations(ReservationRequest request) {
        QueryRequest userReservationsQuery = QueryRequest.builder()
                .tableName(DBConstants.RESERVATIONS_TABLE)
                .indexName(DBConstants.USER_ID_INDEX)
                .keyConditionExpression("userId = :userId")
                .filterExpression("startTimestamp <= :end AND endTimestamp >= :start")
                .expressionAttributeValues(Map.of(
                        ":userId", AttributeValue.builder().s(request.getUserId()).build(),
                        ":start", AttributeValue.builder().n(String.valueOf(request.getStartTimestamp())).build(),
                        ":end", AttributeValue.builder().n(String.valueOf(request.getEndTimestamp())).build()
                ))
                .limit(1)
                .build();

        try {
            QueryResponse userReservations = dynamoDbClient.query(userReservationsQuery);
            return userReservations.count() > 0;
        } catch (DynamoDbException e) {
            log.error("Failed to reserve the slot in DynamoDB", e);
            throw new ReservationServiceException("Failed to reserve the slot in DynamoDB");
        }

    }

    private void createReservationInDynamoDB(int parkingLotId, ReservationRequest request, int spotId, int reservationId) {
        PutItemRequest putItemRequest = PutItemRequest.builder()
                .tableName(DBConstants.RESERVATIONS_TABLE)
                .item(Map.of(
                        DBConstants.RESERVATION_ID, AttributeValue.builder().n(String.valueOf(reservationId)).build(),
                        DBConstants.PARKING_LOT_ID, AttributeValue.builder().n(String.valueOf(parkingLotId)).build(),
                        DBConstants.USER_ID, AttributeValue.builder().s(request.getUserId()).build(),
                        DBConstants.SPOT_ID, AttributeValue.builder().n(String.valueOf(spotId)).build(),
                        DBConstants.START_TIMESTAMP, AttributeValue.builder().n(String.valueOf(request.getStartTimestamp())).build(),
                        DBConstants.END_TIMESTAMP, AttributeValue.builder().n(String.valueOf(request.getEndTimestamp())).build()
                ))
                .build();

        try {
            dynamoDbClient.putItem(putItemRequest);
        } catch (DynamoDbException e) {
            log.error("Failed to reserve the slot in DynamoDB", e);
            throw new ReservationServiceException("Failed to reserve the slot in DynamoDB");
        }
    }

    private void publishReservationEvent(int reservationId) {
        PutEventsRequest putEventsRequest = PutEventsRequest.builder()
                .entries(PutEventsRequestEntry.builder()
                        .eventBusName(DBConstants.EVENT_BUS_NAME)
                        .source("com.parkhere.reservation")
                        .detailType("ReservationCreated")
                        .detail("{\"reservationId\":\"" + reservationId + "\"}")
                        .build())
                .build();

        try {
            eventBridgeClient.putEvents(putEventsRequest);
        } catch (RuntimeException e) {
            log.error("Failed to publish reservation event", e);
            throw new ReservationServiceException("Failed to publish reservation event");
        }
    }
}