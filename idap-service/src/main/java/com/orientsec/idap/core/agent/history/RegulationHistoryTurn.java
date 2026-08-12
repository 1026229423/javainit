package com.orientsec.idap.core.agent.history;

import java.util.LinkedHashMap;
import java.util.Map;

public class RegulationHistoryTurn {
    private String queryId;
    private String question;
    private String answer;
    private String createdAt;
    private Map<String, Object> responseSnapshot = new LinkedHashMap<>();

    public String getQueryId() { return queryId; }
    public void setQueryId(String queryId) { this.queryId = queryId; }
    public String getQuestion() { return question; }
    public void setQuestion(String question) { this.question = question; }
    public String getAnswer() { return answer; }
    public void setAnswer(String answer) { this.answer = answer; }
    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
    public Map<String, Object> getResponseSnapshot() { return responseSnapshot; }
    public void setResponseSnapshot(Map<String, Object> responseSnapshot) { this.responseSnapshot = responseSnapshot; }
}
