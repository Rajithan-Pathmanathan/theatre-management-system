package com.vijaya.theatre.show;

import com.vijaya.theatre.show.dto.PublicSeatResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
interface PublicSeatMapper {

    @Mapping(source = "id", target = "seatId") // the allocation's own id, not seat.id — see PublicSeatResponse's Javadoc
    @Mapping(source = "seat.seatNumber", target = "seatNumber")
    @Mapping(source = "seat.rowLabel", target = "rowLabel")
    @Mapping(source = "seat.seatInRow", target = "seatInRow")
    @Mapping(source = "seat.zone", target = "zone")
    PublicSeatResponse toResponse(ShowSeatAllocation allocation);
}
