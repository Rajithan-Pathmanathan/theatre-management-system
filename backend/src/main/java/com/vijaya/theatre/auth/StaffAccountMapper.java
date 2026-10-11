package com.vijaya.theatre.auth;

import com.vijaya.theatre.auth.dto.StaffProfileResponse;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
interface StaffAccountMapper {

    StaffProfileResponse toProfileResponse(StaffAccount account);
}
