package com.sanjay.aisecurity.dto;

import lombok.Data;
import java.util.List;

@Data
public class ExportRequest {
    private List<String> datasets;
    private String format; // "excel", "csv", or "pdf"
}
