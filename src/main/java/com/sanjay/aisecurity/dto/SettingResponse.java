package com.sanjay.aisecurity.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.sanjay.aisecurity.entity.ApplicationSetting;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO for ApplicationSetting responses.
 * Never exposes sensitive raw values for security-sensitive keys.
 *
 * @author Sanjay
 * @version 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SettingResponse {

    private Long id;
    private String settingKey;
    private String settingValue;
    private String category;
    private String description;
    private boolean editable;
    private String valueType;
    private String updatedBy;
    private LocalDateTime updatedAt;

    public static SettingResponse from(ApplicationSetting setting) {
        // Mask API keys and secrets in response — show only last 6 chars if present
        String value = setting.getSettingValue();
        String key = setting.getSettingKey();
        if (key != null && (key.contains("secret") || key.contains("api_key") || key.contains("password"))
                && value != null && value.length() > 6) {
            value = "••••••" + value.substring(value.length() - 6);
        }

        return SettingResponse.builder()
                .id(setting.getId())
                .settingKey(setting.getSettingKey())
                .settingValue(value)
                .category(setting.getCategory())
                .description(setting.getDescription())
                .editable(setting.isEditable())
                .valueType(setting.getValueType())
                .updatedBy(setting.getUpdatedBy())
                .updatedAt(setting.getUpdatedAt())
                .build();
    }
}
