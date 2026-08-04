package com.orientsec.idap.core.controller;

import com.orientsec.idap.core.policycompare.PolicyCompareTaskCreateRequest;
import com.orientsec.idap.core.policycompare.PolicyCompareTaskPageView;
import com.orientsec.idap.core.policycompare.PolicyCompareTaskService;
import com.orientsec.idap.core.policycompare.PolicyCompareTaskView;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * Stable Java boundary for policy comparison. Execution remains intentionally
 * pending until task-runtime publishes the corresponding task kinds.
 */
@RestController
@RequestMapping("/api/v1/policy-compare/tasks")
public class PolicyCompareTaskController {
    private final PolicyCompareTaskService taskService;

    @Autowired
    public PolicyCompareTaskController(PolicyCompareTaskService taskService) {
        this.taskService = taskService;
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> create(@RequestBody(required = false) PolicyCompareTaskCreateRequest request) {
        try {
            PolicyCompareTaskView task = taskService.create(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(task);
        } catch (PolicyCompareTaskService.ValidationException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(error("POLICY_COMPARE_INVALID_REQUEST", e.getMessage()));
        }
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> list(@RequestParam(required = false) String mode,
                                  @RequestParam(defaultValue = "1") int page,
                                  @RequestParam(defaultValue = "20") int pageSize) {
        try {
            PolicyCompareTaskPageView tasks = taskService.list(mode, page, pageSize);
            return ResponseEntity.ok(tasks);
        } catch (PolicyCompareTaskService.ValidationException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(error("POLICY_COMPARE_INVALID_REQUEST", e.getMessage()));
        }
    }

    @GetMapping(value = "/{taskId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> detail(@PathVariable String taskId) {
        PolicyCompareTaskView task = taskService.find(taskId);
        if (task == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(error("POLICY_COMPARE_TASK_NOT_FOUND", "比对任务不存在或当前服务已重启。"));
        }
        return ResponseEntity.ok(task);
    }

    private Map<String, Object> error(String code, String message) {
        Map<String, Object> error = new HashMap<>();
        error.put("code", code);
        error.put("message", message);
        Map<String, Object> body = new HashMap<>();
        body.put("error", error);
        return body;
    }
}
