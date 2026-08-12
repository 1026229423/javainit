package com.orientsec.idap.core.policycompare;

import lombok.Data;

/** Browser-safe object reference; storage credentials never leave Java. */
@Data
public class PolicyCompareUploadView {
    private String fileId;
    private String uploadId;
    private String objectKey;
    private String filename;
    private String contentType;
    private long size;
}
