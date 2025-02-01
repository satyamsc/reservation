package com.parkhere.reservation.controller;

import com.parkhere.reservation.exception.ReservationServiceException;
import com.parkhere.reservation.model.ReservationRequest;
import com.parkhere.reservation.model.ReservationResponse;
import com.parkhere.reservation.service.ReservationService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ReservationController {

    @Autowired
    private ReservationService reservationService;

    @PostMapping("/api/parking-lots/{parkingLotId}/reservations")
    public ResponseEntity<ReservationResponse> createReservation(@PathVariable int parkingLotId,
                                                                 @Valid @RequestBody ReservationRequest request) {
        ReservationResponse response = reservationService.createReservation(parkingLotId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}