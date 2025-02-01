package com.parkhere.reservation.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ReservationResponse {
    private int reservationId;
    private int spotId;
    private long startTimestamp;
    private long endTimestamp;
}
