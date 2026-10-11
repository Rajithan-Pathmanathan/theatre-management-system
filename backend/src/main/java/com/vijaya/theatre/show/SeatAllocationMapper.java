package com.vijaya.theatre.show;

import com.vijaya.theatre.show.dto.SeatAllocationItemResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
interface SeatAllocationMapper {

    @Mapping(source = "seat.id", target = "seatId")
    @Mapping(source = "seat.seatNumber", target = "seatNumber")
    @Mapping(source = "seat.rowLabel", target = "rowLabel")
    @Mapping(source = "seat.seatInRow", target = "seatInRow")
    @Mapping(source = "seat.zone", target = "zone")
    SeatAllocationItemResponse toItemResponse(ShowSeatAllocation allocation);
}
