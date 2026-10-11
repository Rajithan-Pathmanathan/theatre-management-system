package com.vijaya.theatre.employee;

import com.vijaya.theatre.auth.StaffAccount;
import com.vijaya.theatre.auth.StaffAccountRepository;
import com.vijaya.theatre.common.exception.BusinessRuleViolationException;
import com.vijaya.theatre.common.exception.ResourceNotFoundException;
import com.vijaya.theatre.employee.dto.EmployeeRequest;
import com.vijaya.theatre.employee.dto.EmployeeResponse;
import com.vijaya.theatre.employee.dto.EmployeeStatusRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * UC-16, BR-09. {@link #updateStatus} is the one place with real logic:
 * deactivating an employee who has a linked staff account also disables
 * that account (Phase 2 definition of done) — reactivating does not
 * re-enable it, since that's a deliberate separate decision for an admin.
 */
@Service
@Transactional
class EmployeeService {

    private final EmployeeRepository employeeRepository;
    private final StaffAccountRepository staffAccountRepository;
    private final EmployeeMapper employeeMapper;

    EmployeeService(EmployeeRepository employeeRepository, StaffAccountRepository staffAccountRepository,
                     EmployeeMapper employeeMapper) {
        this.employeeRepository = employeeRepository;
        this.staffAccountRepository = staffAccountRepository;
        this.employeeMapper = employeeMapper;
    }

    @Transactional(readOnly = true)
    List<EmployeeResponse> list() {
        return employeeRepository.findAll().stream().map(employeeMapper::toResponse).toList();
    }

    @Transactional(readOnly = true)
    EmployeeResponse get(UUID id) {
        return employeeMapper.toResponse(requireEmployee(id));
    }

    EmployeeResponse create(EmployeeRequest request) {
        Employee employee = new Employee();
        employeeMapper.updateEntityFromRequest(request, employee);
        employee.setActive(true);
        employee.setLinkedStaffAccount(resolveLinkedStaffAccount(request.linkedStaffAccountId(), null));
        return employeeMapper.toResponse(employeeRepository.save(employee));
    }

    EmployeeResponse update(UUID id, EmployeeRequest request) {
        Employee employee = requireEmployee(id);
        employeeMapper.updateEntityFromRequest(request, employee);
        employee.setLinkedStaffAccount(resolveLinkedStaffAccount(request.linkedStaffAccountId(), id));
        return employeeMapper.toResponse(employeeRepository.save(employee));
    }

    /** FR-48, Phase 2 DoD — deactivation cascades to the linked staff account; reactivation does not. */
    EmployeeResponse updateStatus(UUID id, EmployeeStatusRequest request) {
        Employee employee = requireEmployee(id);
        employee.setActive(request.active());
        if (!request.active() && employee.getLinkedStaffAccount() != null) {
            StaffAccount staffAccount = employee.getLinkedStaffAccount();
            staffAccount.setActive(false);
            staffAccountRepository.save(staffAccount);
        }
        return employeeMapper.toResponse(employeeRepository.save(employee));
    }

    private StaffAccount resolveLinkedStaffAccount(UUID staffAccountId, UUID currentEmployeeId) {
        if (staffAccountId == null) {
            return null;
        }
        StaffAccount staffAccount = staffAccountRepository.findById(staffAccountId)
                .orElseThrow(() -> new ResourceNotFoundException("Staff account not found: " + staffAccountId));
        employeeRepository.findByLinkedStaffAccountId(staffAccountId)
                .filter(existing -> !existing.getId().equals(currentEmployeeId))
                .ifPresent(existing -> {
                    throw new BusinessRuleViolationException(
                            "Staff account '" + staffAccount.getUsername() + "' is already linked to employee " + existing.getFullName());
                });
        return staffAccount;
    }

    private Employee requireEmployee(UUID id) {
        return employeeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found: " + id));
    }
}
