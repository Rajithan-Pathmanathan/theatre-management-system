package com.vijaya.theatre.movie;

import com.vijaya.theatre.movie.dto.MovieRequest;
import com.vijaya.theatre.movie.dto.MovieResponse;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring")
interface MovieMapper {

    MovieResponse toResponse(Movie movie);

    // status is deliberately never touched here — it only changes via MovieStatusRequest/updateStatus
    @Mapping(target = "status", ignore = true)
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateEntityFromRequest(MovieRequest request, @MappingTarget Movie movie);
}
