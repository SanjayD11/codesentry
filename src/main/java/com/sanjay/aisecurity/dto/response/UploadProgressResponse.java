package com.sanjay.aisecurity.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * High-level statistical tracking of uploaded files.
 *
 * @author Sanjay
 * @version 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UploadProgressResponse {
    private long totalUploadedFiles;
    private long totalStorageBytes;
    private long sourceCodeFilesCount;
    private long zipArchivesCount;
    private double averageFileSizeBytes;
    private FileSummaryResponse largestFile;
    private FileSummaryResponse latestUpload;
    private Map<String, Long> filesByExtension;
}
