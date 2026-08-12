package com.orientsec.idap.core.controller;

import com.orientsec.idap.core.policycompare.PolicyCompareUploadService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/policy-compare/uploads")
public class PolicyCompareUploadController {
    private final PolicyCompareUploadService uploadService;

    public PolicyCompareUploadController(PolicyCompareUploadService uploadService) {
        this.uploadService = uploadService;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> upload(@RequestParam("file") MultipartFile file) {
        try {
            return ResponseEntity.status(HttpStatus.CREATED).body(uploadService.upload(file));
        } catch (PolicyCompareUploadService.UploadException e) {
            HttpStatus status = "POLICY_COMPARE_UPLOAD_TOO_LARGE".equals(e.getCode())
                    ? HttpStatus.PAYLOAD_TOO_LARGE : HttpStatus.BAD_REQUEST;
            return ResponseEntity.status(status).body(error(e.getCode(), e.getMessage()));
        }
    }

    private Map<String, Object> error(String code, String message) {
        Map<String, Object> detail = new HashMap<>();
        detail.put("code", code);
        detail.put("message", message);
        Map<String, Object> body = new HashMap<>();
        body.put("error", detail);
        return body;
    }
}
