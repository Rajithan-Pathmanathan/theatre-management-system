package com.vijaya.theatre.canteen;

import com.vijaya.theatre.canteen.dto.SnackItemRequest;
import com.vijaya.theatre.canteen.dto.SnackItemResponse;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring")
interface SnackItemMapper {

    SnackItemResponse toResponse(SnackItem item);

    // available is deliberately never touched here — it only changes via SnackItemAvailabilityRequest
    @Mapping(target = "available", ignore = true)
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateEntityFromRequest(SnackItemRequest request, @MappingTarget SnackItem item);
}
