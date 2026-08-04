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
        Object basisValue = ((Map<String, Object>) answerValue).get("basis");
        if (!(basisValue instanceof Iterable)) return;
        long expiresAt = System.currentTimeMillis() + TTL_MS;
        for (Object item : (Iterable<?>) basisValue) {
            if (!(item instanceof Map)) continue;
            Map<String, Object> citation = new HashMap<>((Map<String, Object>) item);
            Object clauseId = citation.get("clause_id");
            if (clauseId instanceof String && !((String) clauseId).trim().isEmpty()) {
                entries.put((String) clauseId, new Entry(citation, expiresAt));
            }
        }
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
