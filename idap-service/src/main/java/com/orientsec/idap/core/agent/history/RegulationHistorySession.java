package com.orientsec.idap.core.agent.history;

import java.util.ArrayList;
import java.util.List;

public class RegulationHistorySession {
    private String sessionId;
    private String title;
    private String summary;
    private String createdAt;
    private String updatedAt;
    private List<RegulationHistoryTurn> turns = new ArrayList<>();

    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }
    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
    public String getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }
    public List<RegulationHistoryTurn> getTurns() { return turns; }
    public void setTurns(List<RegulationHistoryTurn> turns) { this.turns = turns == null ? new ArrayList<>() : turns; }
}
