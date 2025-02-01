package com.parkhere.reservation.service;

import com.parkhere.reservation.exception.ReservationServiceException;
import com.parkhere.reservation.model.ReservationRequest;
import com.parkhere.reservation.model.ReservationResponse;
import com.parkhere.reservation.util.DBConstants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.DynamoDbException;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
import software.amazon.awssdk.services.dynamodb.model.QueryRequest;
import software.amazon.awssdk.services.dynamodb.model.QueryResponse;
import software.amazon.awssdk.services.eventbridge.EventBridgeClient;
import software.amazon.awssdk.services.eventbridge.model.EventBridgeException;
import software.amazon.awssdk.services.eventbridge.model.PutEventsRequest;
import software.amazon.awssdk.services.eventbridge.model.PutEventsResponse;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ReservationServiceTest {

    @Mock
    private DynamoDbClient dynamoDbClient;

    @Mock
    private EventBridgeClient eventBridgeClient;

    @Mock
    private SpotAllocationService spotAllocationService;

    @InjectMocks
    private ReservationService reservationService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testCreateReservation_Success() {
        ReservationRequest request = new ReservationRequest();
        request.setUserId("user1");
        request.setStartTimestamp(1000L);
        request.setEndTimestamp(2000L);

        QueryResponse queryResponse = QueryResponse.builder().count(0).build();
        when(dynamoDbClient.query(any(QueryRequest.class))).thenReturn(queryResponse);
        PutEventsResponse putEventsResponse = PutEventsResponse.builder().build();
        when(eventBridgeClient.putEvents(any(PutEventsRequest.class))).thenReturn(putEventsResponse);
        when(spotAllocationService.allocateSpot(anyInt(), anyLong(), anyLong()))
                .thenReturn(Map.of(DBConstants.SPOT_ID, 1));
        ReservationResponse response = reservationService.createReservation(1, request);

        assertNotNull(response);
        assertEquals(1, response.getSpotId());
        verify(dynamoDbClient, times(1)).putItem(any(PutItemRequest.class));
        verify(eventBridgeClient, times(1)).putEvents(any(PutEventsRequest.class));
    }

    @Test
    void testCreateReservation_OverlappingReservations() {
        ReservationRequest request = new ReservationRequest();
        request.setUserId("user1");
        request.setStartTimestamp(1000L);
        request.setEndTimestamp(2000L);

        when(dynamoDbClient.query(any(QueryRequest.class))).thenReturn(QueryResponse.builder().count(1).build());
        assertThrows(ReservationServiceException.class, () -> reservationService.createReservation(1, request));
    }

    @Test
    void testCreateReservation_InvalidTimestamps() {
        ReservationRequest request = new ReservationRequest();
        request.setUserId("user1");
        request.setStartTimestamp(2000L);
        request.setEndTimestamp(1000L);
        assertThrows(ReservationServiceException.class, () -> reservationService.createReservation(1, request));
    }

    @Test
    void testCreateReservation_DynamoDbException() {
        ReservationRequest request = new ReservationRequest();
        request.setUserId("user1");
        request.setStartTimestamp(1000L);
        request.setEndTimestamp(2000L);

        when(dynamoDbClient.query(any(QueryRequest.class))).thenThrow(DynamoDbException.builder().message("DynamoDB error").build());
        assertThrows(ReservationServiceException.class, () -> reservationService.createReservation(1, request));
    }

    @Test
    void testCreateReservation_EventBridgeException() {
        ReservationRequest request = new ReservationRequest();
        request.setUserId("user1");
        request.setStartTimestamp(1000L);
        request.setEndTimestamp(2000L);
        QueryResponse queryResponse = QueryResponse.builder().count(0).build();
        when(dynamoDbClient.query(any(QueryRequest.class))).thenReturn(queryResponse);
        when(spotAllocationService.allocateSpot(anyInt(), anyLong(), anyLong()))
                .thenReturn(Map.of(DBConstants.SPOT_ID, 1));

        when(eventBridgeClient.putEvents(any(PutEventsRequest.class)))
                .thenThrow(EventBridgeException.builder().message("The security token included in the request is invalid.").build());

        Exception exception = assertThrows(ReservationServiceException.class, () -> {
            reservationService.createReservation(1, request);
        });
        assertTrue(exception.getMessage().contains("Failed to publish reservation event"));
    }
}