package com.imageplatform.job.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.imageplatform.common.dto.ApiResponse;
import com.imageplatform.common.constants.OperationType;
import com.imageplatform.job.entity.Job;
import com.imageplatform.job.service.JobService;
import com.imageplatform.job.service.FileStorageService;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Collections;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/jobs")
@RequiredArgsConstructor
public class JobController {

    private final JobService jobService;
    private final FileStorageService fileStorageService;
    private final ObjectMapper objectMapper;

    @SneakyThrows
    @PostMapping(consumes = "multipart/form-data")
    public ResponseEntity<ApiResponse<Job>> createJob(
            @RequestParam("file") MultipartFile file,
            @RequestParam("operation") OperationType operation,
            @RequestParam(value = "parameters", required = false) String parametersJson,
            @RequestHeader("X-User-Id") UUID userId) {

        String filePath = fileStorageService.store(file);

        Map<String, String> params = (parametersJson != null && !parametersJson.isBlank())
                ? objectMapper.readValue(parametersJson, new TypeReference<>() {})
                : Collections.emptyMap();

        Job job = Job.builder()
                .userId(userId)
                .originalFileName(file.getOriginalFilename())
                .filePath(filePath)
                .operationType(operation)
                .parameters(params)
                .build();

        Job created = jobService.createJob(job);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok("Job created", created));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Job>> getJob(
            @PathVariable UUID id,
            @RequestHeader("X-User-Id") UUID userId) {
        return ResponseEntity.ok(ApiResponse.ok(jobService.getJobByIdAndUser(id, userId)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<Job>>> listJobs(
            @RequestHeader("X-User-Id") UUID userId,
            Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.ok(jobService.getUserJobs(userId, pageable)));
    }
}
