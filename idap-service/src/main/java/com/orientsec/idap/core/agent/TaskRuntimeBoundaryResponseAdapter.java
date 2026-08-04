package com.orientsec.idap.core.agent;

/** Browser-contract adapter for completed dfzq-pi policy-query runs. */
public class TaskRuntimeBoundaryResponseAdapter extends AuditAiBoundaryResponseAdapter {
    public TaskRuntimeBoundaryResponseAdapter(String sessionId, String queryId, String question) {
        super(sessionId, queryId, question);
    }
}
