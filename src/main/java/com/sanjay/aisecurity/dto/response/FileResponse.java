package com.sanjay.aisecurity.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Detailed File Response DTO.
 *
 * @author Sanjay
 * @version 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FileResponse {
    private Long id;
    private Long projectId;
    private String originalFileName;
    private String fileExtension;
    private String mimeType;
    private long fileSize;
    private String checksumSHA256;
    private String uploadStatus;
    private String scanStatus;
    private LocalDateTime uploadedAt;
    private String uploadedByEmail;
}
