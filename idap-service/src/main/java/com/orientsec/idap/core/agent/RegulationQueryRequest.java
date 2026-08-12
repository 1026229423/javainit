package com.orientsec.idap.core.agent;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.List;

public class RegulationQueryRequest {
    private String operation = "query";
    private String question;

    public String getOperation() {
        return operation == null || operation.trim().isEmpty() ? "query" : operation;
    }

    public void setOperation(String operation) {
        this.operation = operation;
    }

    @JsonProperty("session_id")
    private String sessionId;

    @JsonProperty("attachment_file_ids")
    private List<String> attachmentFileIds = new ArrayList<>();

    private Options options = new Options();

    public String getQuestion() {
        return question;
    }

    public void setQuestion(String question) {
        this.question = question;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public List<String> getAttachmentFileIds() {
        return attachmentFileIds;
    }

    public void setAttachmentFileIds(List<String> attachmentFileIds) {
        this.attachmentFileIds = attachmentFileIds == null ? new ArrayList<String>() : attachmentFileIds;
    }

    public Options getOptions() {
        return options == null ? new Options() : options;
    }

    public void setOptions(Options options) {
        this.options = options;
    }

    public static class Options {
        @JsonProperty("include_superseded")
        private boolean includeSuperseded;

        public boolean isIncludeSuperseded() {
            return includeSuperseded;
        }

        public void setIncludeSuperseded(boolean includeSuperseded) {
            this.includeSuperseded = includeSuperseded;
        }
    }
}
