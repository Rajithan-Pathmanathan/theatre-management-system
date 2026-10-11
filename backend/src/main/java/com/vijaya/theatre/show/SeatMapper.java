package com.vijaya.theatre.show;

import com.vijaya.theatre.show.dto.SeatResponse;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
interface SeatMapper {

    SeatResponse toResponse(Seat seat);
}
