package com.sanjay.aisecurity.dto.request;

import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request body for {@code POST /api/v1/scan/{projectId}}.
 *
 * <p>Wraps the {@link ScanConfigurationDto}. The configuration is optional —
 * if absent, the controller falls back to {@link ScanConfigurationDto#defaults()}.</p>
 *
 * @author Sanjay
 * @version 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TriggerScanRequest {

    /**
     * The complete scan configuration.
     * If null, defaults are applied server-side.
     */
    @Valid
    private ScanConfigurationDto configuration;

    /** Returns the configuration, falling back to defaults if not provided. */
    public ScanConfigurationDto getConfigurationOrDefaults() {
        return configuration != null ? configuration : ScanConfigurationDto.defaults();
    }
}
