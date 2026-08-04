package com.orientsec.idap.core.policycompare;

import lombok.Data;

import java.util.Map;

/** Immutable-by-convention task state returned to the browser. */
@Data
public class PolicyCompareTaskView {
    private String taskId;
    private String mode;
    private String status;
    private String primaryObject;
    private String secondaryObject;
    private PolicyCompareTaskCreateRequest.Scope scope;
    private String createdAt;
    private String updatedAt;
    private String message;
    private Map<String, Object> result;
}
