package com.orientsec.idap.core.controller;

import com.orientsec.idap.core.agent.AuditAiBoundaryResponseAdapter;
import com.orientsec.idap.core.agent.AuditAiQueryClient;
import com.orientsec.idap.core.agent.RegulationQueryRequest;
import com.orientsec.idap.common.model.Result;
import com.orientsec.idap.common.model.ResultGenerator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;

/**
 * Browser-facing regulation-query endpoint. The browser receives one complete
 * JSON response; audit-ai remains an internal stateless dependency.
 */
@RestController
@RequestMapping("/api/v1/regulation")
@Slf4j
public class RegulationQueryController {
    private static final int MAX_QUESTION_LENGTH = 2000;
    private final AuditAiQueryClient auditAiQueryClient;

    @Autowired
    public RegulationQueryController(AuditAiQueryClient auditAiQueryClient) {
        this.auditAiQueryClient = auditAiQueryClient;
    }

    @PostMapping(value = "/queries", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> query(@RequestBody RegulationQueryRequest request) {
        String sessionId = isBlank(request.getSessionId()) ? UUID.randomUUID().toString() : request.getSessionId();
        String queryId = UUID.randomUUID().toString();
        AuditAiBoundaryResponseAdapter adapter = new AuditAiBoundaryResponseAdapter(
                sessionId, queryId, request.getQuestion());

        if (isBlank(request.getQuestion())) {
            return error(HttpStatus.BAD_REQUEST, adapter, "REGULATION_QUERY_INVALID_REQUEST", "问题不能为空。");
        }
        if (request.getQuestion().trim().length() > MAX_QUESTION_LENGTH) {
            return error(HttpStatus.BAD_REQUEST, adapter, "REGULATION_QUERY_INVALID_REQUEST", "问题不能超过2000个字符。");
        }
        if (!request.getAttachmentFileIds().isEmpty()) {
            return error(HttpStatus.BAD_REQUEST, adapter, "REGULATION_QUERY_ATTACHMENT_UNSUPPORTED", "制度查询暂不支持附件。");
        }

        try {
            Map<String, Object> upstream = auditAiQueryClient.query(request, queryId);
            return ResponseEntity.ok(adapter.success(upstream));
        } catch (AuditAiQueryClient.QueryException e) {
            log.warn("regulation query failed, queryId={}, code={}", queryId, e.getCode());
            return error(HttpStatus.SERVICE_UNAVAILABLE, adapter, e.getCode(), e.getMessage());
        }
    }

    @GetMapping(value = "/cases/{caseId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> caseDetail(@PathVariable String caseId) {
        if (isBlank(caseId)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(resourceError("REGULATION_CASE_INVALID_ID", "案例标识不能为空。"));
        }

        try {
            Result<Map<String, Object>> result = ResultGenerator.genSuccessResult(auditAiQueryClient.queryCaseDetail(caseId));
            return ResponseEntity.ok(result);
        } catch (AuditAiQueryClient.QueryException e) {
            HttpStatus status = "REGULATION_CASE_NOT_FOUND".equals(e.getCode())
                    ? HttpStatus.NOT_FOUND : HttpStatus.SERVICE_UNAVAILABLE;
            log.warn("regulation case detail failed, caseId={}, code={}", caseId, e.getCode());
            return ResponseEntity.status(status).body(resourceError(e.getCode(), e.getMessage()));
        }
    }

    @GetMapping(value = "/clauses/{clauseId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> clauseDetail(@PathVariable String clauseId) {
        if (isBlank(clauseId)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(resourceError("REGULATION_CLAUSE_INVALID_ID", "条款标识不能为空。"));
        }

        try {
            Result<Map<String, Object>> result = ResultGenerator.genSuccessResult(auditAiQueryClient.queryClauseDetail(clauseId));
            return ResponseEntity.ok(result);
        } catch (AuditAiQueryClient.QueryException e) {
            HttpStatus status = "REGULATION_CLAUSE_NOT_FOUND".equals(e.getCode())
                    ? HttpStatus.NOT_FOUND : HttpStatus.SERVICE_UNAVAILABLE;
            log.warn("regulation clause detail failed, clauseId={}, code={}", clauseId, e.getCode());
            return ResponseEntity.status(status).body(resourceError(e.getCode(), e.getMessage()));
        }
    }

    private ResponseEntity<Map<String, Object>> error(HttpStatus status, AuditAiBoundaryResponseAdapter adapter,
                                                       String code, String message) {
        return ResponseEntity.status(status).body(adapter.error(code, message));
    }

    private Map<String, Object> resourceError(String code, String message) {
        Map<String, Object> error = new java.util.HashMap<>();
        error.put("code", code);
        error.put("message", message);
        Map<String, Object> body = new java.util.HashMap<>();
        body.put("error", error);
        return body;
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
