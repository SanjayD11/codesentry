package com.sanjay.aisecurity.service;

import com.sanjay.aisecurity.dto.SettingResponse;
import com.sanjay.aisecurity.entity.ApplicationSetting;
import com.sanjay.aisecurity.repository.ApplicationSettingRepository;
import com.sanjay.aisecurity.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Service for managing platform application settings.
 *
 * <p>All settings are stored in the {@code application_settings} table and
 * seeded with sensible defaults on first startup. Changes immediately affect
 * application behaviour without requiring a restart.</p>
 *
 * @author Sanjay
 * @version 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ApplicationSettingsService {

    private final ApplicationSettingRepository settingRepository;

    /**
     * Returns all settings grouped by category.
     *
     * @return map of category → list of settings
     */
    @Transactional(readOnly = true)
    public Map<String, List<SettingResponse>> getAllSettingsGrouped() {
        return settingRepository.findAllByOrderByCategoryAscSettingKeyAsc()
                .stream()
                .map(SettingResponse::from)
                .collect(Collectors.groupingBy(
                        SettingResponse::getCategory,
                        LinkedHashMap::new,
                        Collectors.toList()
                ));
    }

    /**
     * Returns all settings as a flat list.
     */
    @Transactional(readOnly = true)
    public List<SettingResponse> getAllSettings() {
        return settingRepository.findAllByOrderByCategoryAscSettingKeyAsc()
                .stream()
                .map(SettingResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * Updates the value of an editable setting.
     *
     * @param settingKey setting key to update
     * @param newValue   new value
     * @return updated setting response
     * @throws IllegalArgumentException if key not found or setting is read-only
     */
    @Transactional
    public SettingResponse updateSetting(String settingKey, String newValue) {
        ApplicationSetting setting = settingRepository.findBySettingKey(settingKey)
                .orElseThrow(() -> new IllegalArgumentException("Setting not found: " + settingKey));

        if (!setting.isEditable()) {
            throw new IllegalArgumentException("Setting '" + settingKey + "' is read-only and cannot be modified.");
        }

        String adminEmail = SecurityUtils.requireCurrentUserEmail();
        setting.setSettingValue(newValue);
        setting.setUpdatedBy(adminEmail);
        ApplicationSetting saved = settingRepository.save(setting);
        log.info("Setting '{}' updated by admin: {}", settingKey, adminEmail);
        return SettingResponse.from(saved);
    }

    /**
     * Retrieves the raw (unmasked) value of a setting by key.
     * Used internally by services that need to read configuration.
     *
     * @param key          setting key
     * @param defaultValue fallback if not found
     * @return setting value or defaultValue
     */
    @Transactional(readOnly = true)
    public String getValue(String settingKey, String defaultValue) {
        return settingRepository.findBySettingKey(settingKey)
                .map(ApplicationSetting::getSettingValue)
                .orElse(defaultValue);
    }

    /**
     * Seeds default settings on application startup if they don't exist yet.
     */
    @Transactional
    public void seedDefaultSettings() {
        seedIfAbsent("jwt_expiry_hours", "24", "Security",
                "JWT token expiry in hours", "INTEGER", true);
        seedIfAbsent("session_timeout_minutes", "30", "Security",
                "User session timeout in minutes", "INTEGER", true);
        seedIfAbsent("max_failed_login_attempts", "5", "Security",
                "Maximum login attempts before lockout", "INTEGER", true);
        seedIfAbsent("password_min_length", "8", "Authentication",
                "Minimum password length", "INTEGER", true);
        seedIfAbsent("password_require_uppercase", "true", "Authentication",
                "Require at least one uppercase letter in passwords", "BOOLEAN", true);
        seedIfAbsent("password_require_number", "true", "Authentication",
                "Require at least one number in passwords", "BOOLEAN", true);
        seedIfAbsent("email_verification_required", "false", "Authentication",
                "Require email verification before login", "BOOLEAN", true);
        seedIfAbsent("max_upload_size_mb", "20", "Scanning",
                "Maximum file upload size in megabytes", "INTEGER", true);
        seedIfAbsent("max_zip_entries", "500", "Scanning",
                "Maximum number of files inside a ZIP upload", "INTEGER", true);
        seedIfAbsent("allowed_file_extensions", ".java,.kt,.py,.js,.ts,.jsx,.tsx,.xml,.yml,.yaml,.json,.sql,.zip",
                "Scanning", "Comma-separated list of allowed upload extensions", "STRING", true);
        seedIfAbsent("scan_timeout_seconds", "300", "Scanning",
                "Maximum scan duration before auto-cancellation", "INTEGER", true);
        seedIfAbsent("ai_timeout_seconds", "30", "AI",
                "AI provider request timeout in seconds", "INTEGER", true);
        seedIfAbsent("ai_max_tokens", "2048", "AI",
                "Maximum tokens per AI response", "INTEGER", true);
        seedIfAbsent("ai_temperature", "0.2", "AI",
                "AI model temperature (creativity: 0.0 = deterministic, 1.0 = creative)", "STRING", true);
        seedIfAbsent("ai_rate_limit_requests", "10", "AI",
                "Maximum AI requests per minute per user", "INTEGER", true);
        seedIfAbsent("smtp_host", "", "Email",
                "SMTP server hostname for outgoing emails", "STRING", true);
        seedIfAbsent("smtp_port", "587", "Email",
                "SMTP server port", "INTEGER", true);
        seedIfAbsent("smtp_username", "", "Email",
                "SMTP authentication username", "STRING", true);
        seedIfAbsent("smtp_from_address", "", "Email",
                "From email address for platform emails", "STRING", true);
        seedIfAbsent("platform_name", "Aegis Nexus", "General",
                "Platform display name shown in UI and emails", "STRING", true);
        seedIfAbsent("platform_url", "http://localhost:3000", "General",
                "Public URL of the platform", "STRING", true);
        seedIfAbsent("maintenance_mode", "false", "General",
                "When enabled, only admins can access the platform", "BOOLEAN", true);
        seedIfAbsent("user_registration_enabled", "true", "General",
                "Allow new user self-registration", "BOOLEAN", true);
        seedIfAbsent("storage_path", "uploads", "Storage",
                "File system path for uploaded file storage", "STRING", false);
        seedIfAbsent("max_storage_gb", "50", "Storage",
                "Maximum total storage quota in gigabytes", "INTEGER", true);
        log.info("Default application settings seeded.");
    }

    private void seedIfAbsent(String key, String value, String category,
                               String description, String valueType, boolean editable) {
        if (!settingRepository.existsBySettingKey(key)) {
            settingRepository.save(ApplicationSetting.builder()
                    .settingKey(key)
                    .settingValue(value)
                    .category(category)
                    .description(description)
                    .valueType(valueType)
                    .editable(editable)
                    .updatedBy("system")
                    .build());
        }
    }
}
