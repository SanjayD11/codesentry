package com.sanjay.aisecurity.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class SchemaAligner {

    private static final Logger log = LoggerFactory.getLogger(SchemaAligner.class);

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @EventListener(ApplicationReadyEvent.class)
    public void alignSchema() {
        log.info("Checking database schema for stray columns in uploaded_files...");
        try {
            // Expected columns based on the current UploadedFile entity
            List<String> expectedColumns = Arrays.asList(
                    "id", "created_at", "updated_at", "original_filename", "stored_filename",
                    "file_extension", "extension", "mime_type", "file_size", "storage_path",
                    "absolute_path", "relative_path", "checksum_sha256", "upload_status",
                    "scan_status", "is_deleted", "uploaded_at", "project_id", "uploaded_by_id"
            );

            // Get actual columns from DB
            List<Map<String, Object>> columns = jdbcTemplate.queryForList("SHOW COLUMNS FROM uploaded_files");
            List<String> actualColumns = columns.stream()
                    .map(col -> ((String) col.get("Field")).toLowerCase())
                    .collect(Collectors.toList());

            log.info("Found columns in DB: {}", actualColumns);

            // Find columns that are in DB but not in Entity
            List<String> extraColumns = actualColumns.stream()
                    .filter(col -> !expectedColumns.contains(col))
                    .collect(Collectors.toList());

            if (!extraColumns.isEmpty()) {
                log.warn("Found extra columns in uploaded_files: {}. Making them nullable to prevent INSERT crashes.", extraColumns);
                for (String col : extraColumns) {
                    try {
                        // Find the original type
                        String type = columns.stream()
                                .filter(c -> ((String) c.get("Field")).equalsIgnoreCase(col))
                                .map(c -> (String) c.get("Type"))
                                .findFirst().orElse("VARCHAR(255)");
                                
                        // Make it nullable
                        String sql = "ALTER TABLE uploaded_files MODIFY COLUMN " + col + " " + type + " NULL";
                        log.info("Executing: {}", sql);
                        jdbcTemplate.execute(sql);
                    } catch (Exception e) {
                        log.error("Failed to alter column: " + col, e);
                    }
                }
            } else {
                log.info("No stray columns found. Schema is perfectly aligned.");
            }
        } catch (Exception e) {
            log.error("Could not align schema automatically", e);
        }
    }
}
