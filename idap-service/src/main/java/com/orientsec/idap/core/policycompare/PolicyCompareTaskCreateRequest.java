package com.orientsec.idap.core.policycompare;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/** Browser-facing creation input for a policy comparison task. */
@Data
public class PolicyCompareTaskCreateRequest {
    private String mode;
    /** Coverage subject type: external regulation or internal policy. */
    private String coverageSubjectType = "external";
    /** library: selected from policy library; upload: represented by the one attachment id. */
    private String primaryObjectSource = "library";
    private String primaryObject;
    /** Stable normalized knowledge-base version id; required when primaryObjectSource=library. */
    private String primaryDocVersionId;
    /** Old library version used only by same-document version-difference comparison. */
    private String secondaryDocVersionId;
    private String secondaryObject;
    private Scope scope = new Scope();
    private List<String> attachmentFileIds = new ArrayList<>();
    /** MinIO reference returned by Java's upload boundary and consumed by task-runtime. */
    private ExternalArtifact externalArtifact;

    @Data
    public static class Scope {
        private List<String> effectiveDateRange = new ArrayList<>();
    }

    @Data
    public static class ExternalArtifact {
        private String objectKey;
        private String uploadId;
        private String filename;
    }
}
