package com.sanjay.aisecurity.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Summary representation of an uploaded file.
 *
 * @author Sanjay
 * @version 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FileSummaryResponse {
    private Long id;
    private String originalFileName;
    private String fileExtension;
    private long fileSize;
    private String uploadStatus;
    private String scanStatus;
    private LocalDateTime uploadedAt;
}
