package com.vijaya.theatre.canteen;

import com.vijaya.theatre.canteen.dto.SnackItemRequest;
import com.vijaya.theatre.canteen.dto.SnackItemResponse;
import com.vijaya.theatre.common.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/** UC-17, FR-49. */
@Service
@Transactional
class SnackItemService {

    private final SnackItemRepository snackItemRepository;
    private final SnackItemMapper snackItemMapper;

    SnackItemService(SnackItemRepository snackItemRepository, SnackItemMapper snackItemMapper) {
        this.snackItemRepository = snackItemRepository;
        this.snackItemMapper = snackItemMapper;
    }

    @Transactional(readOnly = true)
    List<SnackItemResponse> list() {
        return snackItemRepository.findAll().stream().map(snackItemMapper::toResponse).toList();
    }

    /** UC-18 — only ever offers items the canteen currently has. */
    @Transactional(readOnly = true)
    List<SnackItemResponse> listAvailable() {
        return snackItemRepository.findByAvailableTrue().stream().map(snackItemMapper::toResponse).toList();
    }

    SnackItemResponse create(SnackItemRequest request) {
        SnackItem item = new SnackItem();
        snackItemMapper.updateEntityFromRequest(request, item);
        item.setAvailable(true);
        return snackItemMapper.toResponse(snackItemRepository.save(item));
    }

    SnackItemResponse update(UUID id, SnackItemRequest request) {
        SnackItem item = requireItem(id);
        snackItemMapper.updateEntityFromRequest(request, item);
        return snackItemMapper.toResponse(snackItemRepository.save(item));
    }

    SnackItemResponse updateAvailability(UUID id, boolean available) {
        SnackItem item = requireItem(id);
        item.setAvailable(available);
        return snackItemMapper.toResponse(snackItemRepository.save(item));
    }

    private SnackItem requireItem(UUID id) {
        return snackItemRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Snack item not found: " + id));
    }
}
