package com.vijaya.theatre.employee;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface EmployeeRepository extends JpaRepository<Employee, UUID> {

    /** UC-16 — enforces one employee per staff account with a clean error instead of relying on the DB constraint. */
    Optional<Employee> findByLinkedStaffAccountId(UUID staffAccountId);
}
