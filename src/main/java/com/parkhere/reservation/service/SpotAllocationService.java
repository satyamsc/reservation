package com.parkhere.reservation.service;

import com.parkhere.reservation.exception.ReservationServiceException;
import com.parkhere.reservation.util.DBConstants;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.DynamoDbException;
import software.amazon.awssdk.services.dynamodb.model.QueryRequest;
import software.amazon.awssdk.services.dynamodb.model.QueryResponse;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
@Slf4j
@AllArgsConstructor
public class SpotAllocationService {

    private final DynamoDbClient dynamoDbClient;

    public Map<String, Object> allocateSpot(int parkingLotId, long startTimestamp, long endTimestamp) {
        try {
            QueryRequest queryRequest = QueryRequest.builder()
                    .tableName(DBConstants.PARKING_LOTS_TABLE)
                    .indexName(DBConstants.PARKING_LOT_ID_INDEX)
                    .keyConditionExpression("parkingLotId = :parkingLotId")
                    .expressionAttributeValues(Map.of(
                            ":parkingLotId", AttributeValue.builder().n(String.valueOf(parkingLotId)).build()
                    ))
                    .scanIndexForward(false)
                    .limit(10)
                    .build();

            QueryResponse queryResponse = dynamoDbClient.query(queryRequest);

            Optional<Integer> availableSpotIdOpt = queryResponse.items().stream()
                    .parallel()
                    .filter(item -> isSpotAvailable(parkingLotId, Integer.parseInt(item.get(DBConstants.SPOT_ID).n()), startTimestamp, endTimestamp))
                    .findFirst()
                    .map(item -> Integer.parseInt(item.get(DBConstants.SPOT_ID).n()));

            int availableSpotId = availableSpotIdOpt.orElseThrow(() -> new ReservationServiceException("No spot available in the parking lot."));

            Map<String, Object> result = new HashMap<>();
            result.put("spotId", availableSpotId);
            return result;
        } catch (DynamoDbException e) {
            log.error("Failed to reserve the slot in DynamoDB", e);
            throw new ReservationServiceException("Failed to reserve the slot in DynamoDB. Please try again later.");
        }
    }

    public boolean isSpotAvailable(int parkingLotId, int spotId, long startTimestamp, long endTimestamp) {
        QueryRequest queryRequest = QueryRequest.builder()
                .tableName("Reservations")
                .indexName(DBConstants.PARKING_LOT_ID_INDEX)
                .keyConditionExpression("parkingLotId = :parkingLotId AND spotId = :spotId")
                .filterExpression("(startTimestamp <= :end AND endTimestamp >= :start)")
                .expressionAttributeValues(Map.of(
                        ":parkingLotId", AttributeValue.builder().n(String.valueOf(parkingLotId)).build(),
                        ":spotId", AttributeValue.builder().n(String.valueOf(spotId)).build(),
                        ":start", AttributeValue.builder().n(String.valueOf(startTimestamp)).build(),
                        ":end", AttributeValue.builder().n(String.valueOf(endTimestamp)).build()
                ))
                .limit(1)
                .build();

        try {
            QueryResponse response = dynamoDbClient.query(queryRequest);
            return response.count() == 0;
        } catch (DynamoDbException e) {
            log.error("DynamoDB Query Error: {}", e.getMessage(), e);
            throw new ReservationServiceException("Unable to check spot availability");
        }
    }
}