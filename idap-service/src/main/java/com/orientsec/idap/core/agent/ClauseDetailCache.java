package com.orientsec.idap.core.agent;

import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Short-lived allowlist of clauses that were returned by a completed task-runtime query. */
@Component
public class ClauseDetailCache {
    private static final long TTL_MS = 15 * 60 * 1000L;
    private final Map<String, Entry> entries = new ConcurrentHashMap<>();

    @SuppressWarnings("unchecked")
    public void remember(Map<String, Object> upstream) {
        Object answerValue = upstream.get("answer");
        if (!(answerValue instanceof Map)) return;
        Map<String, Object> answer = (Map<String, Object>) answerValue;
        Object basisValue = answer.get("basis");
        if (!(basisValue instanceof Iterable)) return;
        Map<String, Map<String, Object>> detailsByClause = sourceDetailsByClause(answer.get("source_details"));
        long expiresAt = System.currentTimeMillis() + TTL_MS;
        for (Object item : (Iterable<?>) basisValue) {
            if (!(item instanceof Map)) continue;
            Map<String, Object> citation = new HashMap<>((Map<String, Object>) item);
            Object clauseId = citation.get("clause_id");
            if (clauseId instanceof String && !((String) clauseId).trim().isEmpty()) {
                Map<String, Object> detail = detailsByClause.get(clauseId);
                if (detail != null) citation.putAll(detail);
                entries.put((String) clauseId, new Entry(citation, expiresAt));
            }
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Map<String, Object>> sourceDetailsByClause(Object value) {
        Map<String, Map<String, Object>> result = new HashMap<>();
        if (!(value instanceof Iterable)) return result;
        for (Object item : (Iterable<?>) value) {
            if (!(item instanceof Map)) continue;
            Map<String, Object> detail = new HashMap<>((Map<String, Object>) item);
            Object clauseId = detail.get("clause_id");
            if (clauseId instanceof String && !((String) clauseId).trim().isEmpty()) {
                result.put((String) clauseId, detail);
            }
        }
        return result;
    }

    public Map<String, Object> find(String clauseId) {
        Entry entry = entries.get(clauseId);
        if (entry == null) return null;
        if (entry.expiresAt < System.currentTimeMillis()) {
            entries.remove(clauseId, entry);
            return null;
        }
        return new HashMap<>(entry.citation);
    }

    private static final class Entry {
        private final Map<String, Object> citation;
        private final long expiresAt;

        private Entry(Map<String, Object> citation, long expiresAt) {
            this.citation = citation;
            this.expiresAt = expiresAt;
        }
    }
}
