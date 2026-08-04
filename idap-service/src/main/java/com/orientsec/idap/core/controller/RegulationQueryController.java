package com.orientsec.idap.core.controller;

import com.orientsec.idap.core.agent.TaskRuntimeQueryClient;
import com.orientsec.idap.core.agent.TaskRuntimeBoundaryResponseAdapter;
import com.orientsec.idap.core.agent.RegulationQueryRequest;
import com.orientsec.idap.core.agent.ClauseDetailCache;
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
 * JSON response; dfzq-pi task-runtime remains an internal stateless dependency.
 */
@RestController
@RequestMapping("/api/v1/regulation")
@Slf4j
public class RegulationQueryController {
    private static final int MAX_QUESTION_LENGTH = 2000;
    private final TaskRuntimeQueryClient taskRuntimeQueryClient;
    private final ClauseDetailCache clauseDetailCache;

    @Autowired
    public RegulationQueryController(TaskRuntimeQueryClient taskRuntimeQueryClient, ClauseDetailCache clauseDetailCache) {
        this.taskRuntimeQueryClient = taskRuntimeQueryClient;
        this.clauseDetailCache = clauseDetailCache;
    }

    @PostMapping(value = "/queries", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> query(@RequestBody RegulationQueryRequest request) {
        String sessionId = isBlank(request.getSessionId()) ? UUID.randomUUID().toString() : request.getSessionId();
        String queryId = UUID.randomUUID().toString();
        TaskRuntimeBoundaryResponseAdapter adapter = new TaskRuntimeBoundaryResponseAdapter(
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
            Map<String, Object> upstream = taskRuntimeQueryClient.query(request, queryId);
            clauseDetailCache.remember(upstream);
            return ResponseEntity.ok(adapter.success(upstream));
        } catch (TaskRuntimeQueryClient.QueryException e) {
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

        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED)
                .body(resourceError("TASK_RUNTIME_DETAIL_UNSUPPORTED", "当前制度查询服务不提供案例详情回查。"));
    }

    @GetMapping(value = "/clauses/{clauseId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> clauseDetail(@PathVariable String clauseId) {
        if (isBlank(clauseId)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(resourceError("REGULATION_CLAUSE_INVALID_ID", "条款标识不能为空。"));
        }

        Map<String, Object> citation = clauseDetailCache.find(clauseId);
        if (citation == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(resourceError("REGULATION_CLAUSE_NOT_AVAILABLE", "条款详情仅可回查本次查询结果，请重新查询后再试。"));
        }
        if (!(citation.get("text") instanceof String) || ((String) citation.get("text")).trim().isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(resourceError("REGULATION_CLAUSE_TEXT_UNAVAILABLE", "本次查询未返回条款原文，请重新查询后再试。"));
        }
        Map<String, Object> detail = new java.util.HashMap<>();
        detail.put("clause_id", clauseId);
        detail.put("doc_title", citation.get("doc_title"));
        detail.put("clause_path", citation.get("clause_path"));
        detail.put("full_text", citation.get("text"));
        detail.put("status", citation.get("status"));
        detail.put("doc_no", citation.get("source_code"));
        return ResponseEntity.ok(resourceSuccess(detail));
    }

    private ResponseEntity<Map<String, Object>> error(HttpStatus status, TaskRuntimeBoundaryResponseAdapter adapter,
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

    private Map<String, Object> resourceSuccess(Map<String, Object> data) {
        Map<String, Object> body = new java.util.HashMap<>();
        body.put("code", 200);
        body.put("message", "success");
        body.put("data", data);
        return body;
    }


    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
