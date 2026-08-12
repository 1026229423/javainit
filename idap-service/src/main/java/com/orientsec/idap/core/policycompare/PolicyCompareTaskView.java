package com.orientsec.idap.core.policycompare;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Immutable-by-convention task state returned to the browser. */
@Data
public class PolicyCompareTaskView {
    private String taskId;
    private String mode;
    private String coverageSubjectType;
    private String status;
    private String primaryObjectSource;
    private String primaryObject;
    private String primaryDocVersionId;
    private String secondaryObject;
    private String secondaryDocVersionId;
    private List<String> attachmentFileIds = new ArrayList<>();
    private PolicyCompareTaskCreateRequest.Scope scope;
    private String createdAt;
    private String updatedAt;
    private String message;
    private Map<String, Object> result;
    private String runtimeRunId;
    private Map<String, Object> progress;
}
