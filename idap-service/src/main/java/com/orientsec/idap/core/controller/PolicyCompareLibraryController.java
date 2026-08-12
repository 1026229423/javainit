package com.orientsec.idap.core.controller;

import com.orientsec.idap.core.policycompare.TaskRuntimePolicyCompareClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/** Browser-safe proxy for the normalized external-regulation catalog owned by audit-ai. */
@RestController
@RequestMapping("/api/v1/policy-compare/library")
public class PolicyCompareLibraryController {
    private final TaskRuntimePolicyCompareClient client;

    @Autowired
    public PolicyCompareLibraryController(TaskRuntimePolicyCompareClient client) {
        this.client = client;
    }

    @GetMapping("/external-documents")
    public ResponseEntity<?> externalDocuments() {
        return documents(false);
    }

    @GetMapping("/internal-documents")
    public ResponseEntity<?> internalDocuments() {
        return documents(true);
    }

    private ResponseEntity<?> documents(boolean internal) {
        try {
            return ResponseEntity.ok(internal ? client.listInternalDocuments() : client.listExternalDocuments());
        } catch (TaskRuntimePolicyCompareClient.CompareException e) {
            Map<String, Object> detail = new HashMap<>();
            detail.put("code", e.getCode());
            detail.put("message", e.getMessage());
            Map<String, Object> body = new HashMap<>();
            body.put("error", detail);
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(body);
        }
    }
}
