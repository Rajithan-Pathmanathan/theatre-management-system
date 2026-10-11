package com.vijaya.theatre.canteen;

import com.vijaya.theatre.canteen.dto.SnackItemAvailabilityRequest;
import com.vijaya.theatre.canteen.dto.SnackItemRequest;
import com.vijaya.theatre.canteen.dto.SnackItemResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/** UC-17. ADMIN only — gated by the {@code /api/admin/**} rule in SecurityConfig. */
@RestController
@RequestMapping("/api/admin/snack-items")
class SnackItemAdminController {

    private final SnackItemService snackItemService;

    SnackItemAdminController(SnackItemService snackItemService) {
        this.snackItemService = snackItemService;
    }

    @GetMapping
    List<SnackItemResponse> list() {
        return snackItemService.list();
    }

    @PostMapping
    ResponseEntity<SnackItemResponse> create(@Valid @RequestBody SnackItemRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(snackItemService.create(request));
    }

    @PutMapping("/{id}")
    SnackItemResponse update(@PathVariable UUID id, @Valid @RequestBody SnackItemRequest request) {
        return snackItemService.update(id, request);
    }

    @PatchMapping("/{id}/availability")
    SnackItemResponse updateAvailability(@PathVariable UUID id, @Valid @RequestBody SnackItemAvailabilityRequest request) {
        return snackItemService.updateAvailability(id, request.available());
    }
}
