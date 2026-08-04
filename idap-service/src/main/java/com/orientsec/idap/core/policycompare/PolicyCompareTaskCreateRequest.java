package com.orientsec.idap.core.policycompare;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/** Browser-facing creation input for a policy comparison task. */
@Data
public class PolicyCompareTaskCreateRequest {
    private String mode;
    private String primaryObject;
    private String secondaryObject;
    private Scope scope = new Scope();
    private List<String> attachmentFileIds = new ArrayList<>();

    @Data
    public static class Scope {
        private List<String> organizations = new ArrayList<>();
        private List<String> businessScenes = new ArrayList<>();
        private List<String> chapters = new ArrayList<>();
        private List<String> effectiveDateRange = new ArrayList<>();
    }
}
