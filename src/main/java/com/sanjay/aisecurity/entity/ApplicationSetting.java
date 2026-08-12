package com.sanjay.aisecurity.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * ApplicationSetting Entity.
 *
 * <p>Persists all platform configuration key-value pairs in the database.
 * Replaces hardcoded configuration values with database-driven settings that
 * can be changed at runtime without restarting the application.</p>
 *
 * @author Sanjay
 * @version 1.0.0
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(
    name = "application_settings",
    indexes = {
        @Index(name = "idx_setting_key", columnList = "setting_key", unique = true),
        @Index(name = "idx_setting_category", columnList = "category")
    }
)
public class ApplicationSetting extends BaseEntity {

    @NotBlank(message = "Setting key is required")
    @Size(max = 100)
    @Column(name = "setting_key", nullable = false, unique = true, length = 100)
    private String settingKey;

    @Column(name = "setting_value", columnDefinition = "TEXT")
    private String settingValue;

    @NotBlank(message = "Category is required")
    @Size(max = 50)
    @Column(name = "category", nullable = false, length = 50)
    private String category;

    @Size(max = 255)
    @Column(name = "description", length = 255)
    private String description;

    @Builder.Default
    @Column(name = "editable", nullable = false)
    private boolean editable = true;

    @Size(max = 50)
    @Column(name = "value_type", length = 50)
    private String valueType; // STRING, INTEGER, BOOLEAN, JSON

    @Column(name = "updated_by", length = 100)
    private String updatedBy;
}
