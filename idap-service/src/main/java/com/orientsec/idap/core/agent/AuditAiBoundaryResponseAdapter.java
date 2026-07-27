package com.orientsec.idap.core.agent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Converts audit-ai's complete JSON response into the browser-facing JSON contract. */
public class AuditAiBoundaryResponseAdapter {
    private final String sessionId;
    private final String queryId;
    private final String question;

    public AuditAiBoundaryResponseAdapter(String sessionId, String queryId, String question) {
        this.sessionId = sessionId;
        this.queryId = queryId;
        this.question = question;
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> success(Map<String, Object> upstream) {
        Map<String, Object> response = new HashMap<>();
        response.put("context", context());
        response.put("answer_blocks", list(upstream.get("answer_blocks")));
        response.put("citations", list(upstream.get("citations")));

        Map<String, Object> meta = map(upstream.get("meta"));
        Map<String, Object> completion = map(upstream.get("completion"));
        Map<String, Object> structured = map(upstream.get("structured"));
        Map<String, Object> regulations = map(structured.get("regulations"));
        Map<String, Object> clauses = map(structured.get("clauses"));
        Map<String, Object> rules = map(structured.get("regulatory_rules"));
        Map<String, Object> cases = map(structured.get("cases"));
        Map<String, Object> result = new HashMap<>();
        result.put("elapsed_ms", meta.get("elapsed_ms"));
        result.put("summary", null);
        result.put("counts", counts(regulations, clauses, rules, cases));
        result.put("regulations", tabItems(regulations));
        result.put("clauses", tabItems(clauses));
        result.put("rules", tabItems(rules));
        result.put("cases", tabItems(cases));
        List<Object> citationAdvice = list(structured.get("citation_advice"));
        result.put("citation_advice", citationAdvice.isEmpty() ? list(upstream.get("citations")) : citationAdvice);
        response.put("result", result);

        Map<String, Object> browserCompletion = new HashMap<>();
        browserCompletion.put("finish_reason", valueOr(completion.get("finish_reason"), "stop"));
        browserCompletion.put("exhausted_scope", valueOr(completion.get("exhausted_scope"), Collections.emptyList()));
        browserCompletion.put("query_id", queryId);
        response.put("completion", browserCompletion);
        return response;
    }

    public Map<String, Object> error(String code, String message) {
        Map<String, Object> error = new HashMap<>();
        error.put("code", code);
        error.put("message", message);
        error.put("query_id", queryId);
        Map<String, Object> response = new HashMap<>();
        response.put("error", error);
        return response;
    }

    private Map<String, Object> context() {
        Map<String, Object> context = new HashMap<>();
        context.put("session_id", sessionId);
        context.put("query_id", queryId);
        context.put("current_question", question);
        context.put("route_type", "institution_query");
        context.put("hit_skill", "制度查询");
        context.put("knowledge_scope", Collections.emptyList());
        context.put("review", Collections.singletonMap("required", false));
        return context;
    }

    private Map<String, Object> counts(Map<String, Object> regulations, Map<String, Object> clauses,
                                       Map<String, Object> rules, Map<String, Object> cases) {
        Map<String, Object> counts = new HashMap<>();
        counts.put("regulations", tabCount(regulations));
        counts.put("clauses", tabCount(clauses));
        counts.put("rules", tabCount(rules));
        counts.put("cases", tabCount(cases));
        return counts;
    }

    private List<Object> tabItems(Map<String, Object> tab) {
        return list(tab.get("items"));
    }

    private int tabCount(Map<String, Object> tab) {
        Object total = tab.get("total");
        if (total instanceof Number) {
            return ((Number) total).intValue();
        }
        return tabItems(tab).size();
    }

    @SuppressWarnings("unchecked")
    private List<Object> list(Object value) {
        return value instanceof List ? new ArrayList<>((List<Object>) value) : Collections.emptyList();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> map(Object value) {
        return value instanceof Map ? (Map<String, Object>) value : Collections.<String, Object>emptyMap();
    }

    private Object valueOr(Object value, Object fallback) {
        return value == null ? fallback : value;
    }
}
