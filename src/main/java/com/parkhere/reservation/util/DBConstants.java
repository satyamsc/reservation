package com.parkhere.reservation.util;

import lombok.experimental.UtilityClass;

@UtilityClass
public class DBConstants{
    public static final String EVENT_BUS_NAME = "reservation-events";
    public static final String SPOT_ID = "spotId";
    public static final String RESERVATION_ID = "reservationId";
    public static final String PARKING_LOT_ID = "parkingLotId";
    public static final String USER_ID = "userId";
    public static final String START_TIMESTAMP = "startTimestamp";
    public static final String END_TIMESTAMP = "endTimestamp";
    public static final String PARKING_LOTS_TABLE = "ParkingLots";
    public static final String PARKING_LOT_ID_INDEX = "ParkingLotIdIndex";

    public static final String RESERVATIONS_TABLE = "Reservations";
    public static final String USER_ID_INDEX = "UserIdIndex";

}
