package com.vijaya.theatre.employee;

import com.vijaya.theatre.employee.dto.EmployeeRequest;
import com.vijaya.theatre.employee.dto.EmployeeResponse;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring")
interface EmployeeMapper {

    @Mapping(source = "linkedStaffAccount.id", target = "linkedStaffAccountId")
    @Mapping(source = "linkedStaffAccount.username", target = "linkedStaffAccountUsername")
    EmployeeResponse toResponse(Employee employee);

    // linkedStaffAccount is resolved and set separately in the service (needs a repository lookup); active never
    // changes here either — it only changes via EmployeeStatusRequest/updateStatus, same convention as Movie/Show.
    @Mapping(target = "linkedStaffAccount", ignore = true)
    @Mapping(target = "active", ignore = true)
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateEntityFromRequest(EmployeeRequest request, @MappingTarget Employee employee);
}
