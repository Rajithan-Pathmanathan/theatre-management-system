package com.vijaya.theatre.show;

import com.vijaya.theatre.show.dto.ShowResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
interface ShowMapper {

    @Mapping(source = "movie.id", target = "movieId")
    @Mapping(source = "movie.title", target = "movieTitle")
    ShowResponse toResponse(Show show);
}
