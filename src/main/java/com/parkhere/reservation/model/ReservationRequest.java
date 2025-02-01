package com.parkhere.reservation.model;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReservationRequest {
    @NotNull(message = "User ID cannot be null")
    private String userId;
    @NotNull(message = "Start timestamp cannot be null")
    @Min(value = 0, message = "Start timestamp must be a positive number")
    private long startTimestamp;
    @NotNull(message = "End timestamp cannot be null")
    @Min(value = 0, message = "End timestamp must be a positive number")
    private long endTimestamp;

}