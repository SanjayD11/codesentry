package com.sanjay.aisecurity.controller;

import com.sanjay.aisecurity.entity.UploadedFile;
import com.sanjay.aisecurity.repository.UploadedFileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/test2")
@RequiredArgsConstructor
public class TestQueryController {

    private final UploadedFileRepository uploadedFileRepository;

    private final com.sanjay.aisecurity.repository.ScanHistoryRepository scanHistoryRepository;

    @GetMapping("/files")
    public String getFiles() {
        List<UploadedFile> files = uploadedFileRepository.findAll();
        StringBuilder sb = new StringBuilder();
        sb.append("Total files: ").append(files.size()).append("\n");
        for (UploadedFile f : files) {
            sb.append("File ID: ").append(f.getId())
              .append(", Name: ").append(f.getOriginalFileName())
              .append(", Ext: ").append(f.getFileExtension())
              .append(", Proj ID: ").append(f.getProject().getId())
              .append(", Deleted: ").append(f.isDeleted())
              .append("\n");
        }
        sb.append("\n--- Scans ---\n");
        List<com.sanjay.aisecurity.entity.ScanHistory> scans = scanHistoryRepository.findAll();
        for (com.sanjay.aisecurity.entity.ScanHistory s : scans) {
            sb.append("Scan ID: ").append(s.getId())
              .append(", Status: ").append(s.getStatus())
              .append(", Scanned: ").append(s.getScannedFiles())
              .append(", Vulns: ").append(s.getTotalVulnerabilities())
              .append("\n");
        }
        return sb.toString();
    }

    @GetMapping("/undelete-all")
    public String undeleteAll() {
        List<UploadedFile> files = uploadedFileRepository.findAll();
        int count = 0;
        for (UploadedFile f : files) {
            if (f.isDeleted()) {
                f.setDeleted(false);
                uploadedFileRepository.save(f);
                count++;
            }
        }
        return "Undeleted " + count + " files.";
    }
}
