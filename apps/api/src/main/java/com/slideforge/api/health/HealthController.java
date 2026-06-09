package com.slideforge.api.health;

import com.slideforge.api.common.response.ApiResponse;
import java.time.Instant;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/health")
public class HealthController {

    @GetMapping
    public ApiResponse<HealthResponse> health() {
        return ApiResponse.success(new HealthResponse("UP", "slideforge-api", Instant.now().toString()));
    }

    public record HealthResponse(String status, String service, String checkedAt) {
    }
}
