package com.slideforge.api.settings;

import com.slideforge.api.common.response.ApiResponse;
import com.slideforge.api.settings.dto.AiConnectionTestResponse;
import com.slideforge.api.settings.dto.AiSettingsResponse;
import com.slideforge.api.settings.dto.UpdateAiSettingsRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/settings/ai")
public class AiSettingsController {

    private final AiSettingsService aiSettingsService;

    public AiSettingsController(AiSettingsService aiSettingsService) {
        this.aiSettingsService = aiSettingsService;
    }

    @GetMapping
    public ApiResponse<AiSettingsResponse> getSettings() {
        return ApiResponse.success(aiSettingsService.getSettings());
    }

    @PutMapping
    public ApiResponse<AiSettingsResponse> updateSettings(@Valid @RequestBody UpdateAiSettingsRequest request) {
        return ApiResponse.success(aiSettingsService.updateSettings(request));
    }

    @PostMapping("/test")
    public ApiResponse<AiConnectionTestResponse> testConnection(@RequestBody(required = false) UpdateAiSettingsRequest request) {
        return ApiResponse.success(aiSettingsService.testConnection(request));
    }

    @DeleteMapping("/key")
    public ApiResponse<AiSettingsResponse> deleteApiKey() {
        return ApiResponse.success(aiSettingsService.deleteApiKey());
    }
}
