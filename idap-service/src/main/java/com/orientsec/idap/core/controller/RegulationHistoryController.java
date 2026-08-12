package com.orientsec.idap.core.controller;

import com.orientsec.idap.core.agent.history.RegulationHistoryService;
import com.orientsec.idap.core.agent.history.RegulationHistorySession;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/regulation/sessions")
public class RegulationHistoryController {
    private final RegulationHistoryService historyService;

    public RegulationHistoryController(RegulationHistoryService historyService) {
        this.historyService = historyService;
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(name = "page_size", defaultValue = "20") int pageSize) {
        try {
            return ResponseEntity.ok(success(historyService.list(page, pageSize)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error("REGULATION_HISTORY_INVALID_PAGE", e.getMessage()));
        }
    }

    @GetMapping(value = "/{sessionId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> detail(@PathVariable String sessionId) {
        try {
            RegulationHistorySession session = historyService.find(sessionId);
            if (session == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(error("REGULATION_HISTORY_NOT_FOUND", "历史会话不存在。"));
            }
            return ResponseEntity.ok(success(session));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(error("REGULATION_HISTORY_INVALID_SESSION", e.getMessage()));
        }
    }

    private Map<String, Object> success(Object data) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("code", 200);
        body.put("message", "success");
        body.put("data", data);
        return body;
    }

    private Map<String, Object> error(String code, String message) {
        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("code", code);
        detail.put("message", message);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("error", detail);
        return body;
    }
}
