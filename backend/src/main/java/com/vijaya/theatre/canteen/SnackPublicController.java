package com.vijaya.theatre.canteen;

import com.vijaya.theatre.canteen.dto.SnackItemResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** UC-18, BR-08. Public — no booking or account needed; gated as permitAll in SecurityConfig. */
@RestController
@RequestMapping("/api/snacks")
class SnackPublicController {

    private final SnackItemService snackItemService;

    SnackPublicController(SnackItemService snackItemService) {
        this.snackItemService = snackItemService;
    }

    @GetMapping
    List<SnackItemResponse> list() {
        return snackItemService.listAvailable();
    }
}
