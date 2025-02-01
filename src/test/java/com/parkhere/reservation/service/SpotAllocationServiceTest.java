package com.parkhere.reservation.service;

import com.parkhere.reservation.exception.ReservationServiceException;
import com.parkhere.reservation.util.DBConstants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.DynamoDbException;
import software.amazon.awssdk.services.dynamodb.model.QueryRequest;
import software.amazon.awssdk.services.dynamodb.model.QueryResponse;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class SpotAllocationServiceTest {

    @Mock
    private DynamoDbClient dynamoDbClient;

    @InjectMocks
    private SpotAllocationService spotAllocationService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testAllocateSpot_Success() {
        QueryResponse queryResponse = mock(QueryResponse.class);
        when(queryResponse.items()).thenReturn(List.of(
                Map.of(DBConstants.SPOT_ID, AttributeValue.builder().n("1").build()),
                Map.of(DBConstants.SPOT_ID, AttributeValue.builder().n("2").build())
        ));
        when(dynamoDbClient.query(any(QueryRequest.class))).thenReturn(queryResponse);

        SpotAllocationService spyService = spy(spotAllocationService);
        doReturn(true).when(spyService).isSpotAvailable(anyInt(), anyInt(), anyLong(), anyLong());

        Map<String, Object> result = spyService.allocateSpot(1, 1000L, 2000L);
        assertNotNull(result);
        assertTrue(result.containsKey("spotId"));
        assertEquals(1, result.get("spotId"));
    }

    @Test
    void testAllocateSpot_NoAvailableSpots() {
        QueryResponse queryResponse = mock(QueryResponse.class);
        when(queryResponse.items()).thenReturn(List.of(
                Map.of(DBConstants.SPOT_ID, AttributeValue.builder().n("1").build()),
                Map.of(DBConstants.SPOT_ID, AttributeValue.builder().n("2").build())
        ));
        when(dynamoDbClient.query(any(QueryRequest.class))).thenReturn(queryResponse);
        SpotAllocationService spyService = spy(spotAllocationService);
        doReturn(false).when(spyService).isSpotAvailable(anyInt(), anyInt(), anyLong(), anyLong());
        Exception exception = assertThrows(ReservationServiceException.class, () -> {
            spyService.allocateSpot(1, 1000L, 2000L);
        });

        assertEquals("No spot available in the parking lot.", exception.getMessage());
    }

    @Test
    void testAllocateSpot_DynamoDbException() {
        when(dynamoDbClient.query(any(QueryRequest.class))).thenThrow(DynamoDbException.builder().message("DynamoDB error").build());

        Exception exception = assertThrows(ReservationServiceException.class, () -> {
            spotAllocationService.allocateSpot(1, 1000L, 2000L);
        });
        assertEquals("Failed to reserve the slot in DynamoDB. Please try again later.", exception.getMessage());
    }

    @Test
    void testIsSpotAvailable_Success() {
        QueryResponse queryResponse = mock(QueryResponse.class);
        when(queryResponse.count()).thenReturn(0);
        when(dynamoDbClient.query(any(QueryRequest.class))).thenReturn(queryResponse);
        boolean isAvailable = spotAllocationService.isSpotAvailable(1, 1, 1000L, 2000L);
        assertTrue(isAvailable);
    }

    @Test
    void testIsSpotAvailable_NotAvailable() {
        QueryResponse queryResponse = mock(QueryResponse.class);
        when(queryResponse.count()).thenReturn(1);
        when(dynamoDbClient.query(any(QueryRequest.class))).thenReturn(queryResponse);
        boolean isAvailable = spotAllocationService.isSpotAvailable(1, 1, 1000L, 2000L);
        assertFalse(isAvailable);
    }

    @Test
    void testIsSpotAvailable_DynamoDbException() {
        when(dynamoDbClient.query(any(QueryRequest.class))).thenThrow(DynamoDbException.builder().message("DynamoDB error").build());

        Exception exception = assertThrows(ReservationServiceException.class, () -> {
            spotAllocationService.isSpotAvailable(1, 1, 1000L, 2000L);
        });
        assertEquals("Unable to check spot availability", exception.getMessage());
    }
}