package com.sanjay.aisecurity.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SystemHealthResponse {
    private String backendStatus;
    private String databaseStatus;
    private String aiProviderStatus;
    private String storageUsage;
}
