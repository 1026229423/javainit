package com.orientsec.idap.core.agent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Converts task-runtime's completed policy-query response into the browser-facing JSON contract. */
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
        Map<String, Object> answer = map(upstream.get("answer"));
        List<Object> basis = list(answer.get("basis"));

        Map<String, Object> answerBlock = new HashMap<>();
        answerBlock.put("block_seq", 0);
        answerBlock.put("block_type", "text");
        answerBlock.put("content", valueOr(answer.get("conclusion"), ""));
        response.put("answer_blocks", Collections.<Object>singletonList(answerBlock));
        response.put("citations", basis);

        Map<String, Object> completion = answer;
        Map<String, Object> detailsByClause = sourceDetailsByClause(answer.get("source_details"));
        List<Object> regulations = regulations(basis);
        List<Object> clauses = clauses(basis);
        List<Object> rules = rules(basis, detailsByClause);
        List<Object> cases = Collections.emptyList();
        Map<String, Object> result = new HashMap<>();
        result.put("elapsed_ms", null);
        result.put("summary", null);
        result.put("counts", counts(regulations, clauses, rules, cases));
        result.put("regulations", regulations);
        result.put("clauses", clauses);
        result.put("rules", rules);
        result.put("cases", cases);
        result.put("citation_advice", basis);
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

    private Map<String, Object> counts(List<Object> regulations, List<Object> clauses,
                                       List<Object> rules, List<Object> cases) {
        Map<String, Object> counts = new HashMap<>();
        counts.put("regulations", regulations.size());
        counts.put("clauses", clauses.size());
        counts.put("rules", rules.size());
        counts.put("cases", cases.size());
        return counts;
    }

    private List<Object> regulations(List<Object> basis) {
        Map<String, Object> byDocument = new LinkedHashMap<>();
        for (Object item : basis) {
            Map<String, Object> citation = map(item);
            if (!"internal".equals(stringValue(citation.get("corpus_type")))) {
                continue;
            }
            String documentId = stringValue(citation.get("source_doc_id"));
            if (documentId.length() == 0) {
                documentId = stringValue(citation.get("doc_title"));
            }
            if (documentId.length() == 0 || byDocument.containsKey(documentId)) {
                continue;
            }
            Map<String, Object> row = new HashMap<>();
            row.put("doc_id", documentId);
            row.put("doc_title", citation.get("doc_title"));
            row.put("doc_no", citation.get("source_code"));
            row.put("clause_excerpt", citation.get("clause_path"));
            row.put("match_score", citation.get("score"));
            byDocument.put(documentId, row);
        }
        return new ArrayList<Object>(byDocument.values());
    }

    private List<Object> clauses(List<Object> basis) {
        List<Object> rows = new ArrayList<>();
        for (Object item : basis) {
            Map<String, Object> citation = map(item);
            if (!"internal".equals(stringValue(citation.get("corpus_type")))) {
                continue;
            }
            Map<String, Object> row = new HashMap<>();
            row.put("clause_id", citation.get("clause_id"));
            row.put("doc_title", citation.get("doc_title"));
            row.put("clause_path", citation.get("clause_path"));
            row.put("snippet", citation.get("clause_path"));
            row.put("theme", citation.get("corpus_type"));
            row.put("match_score", citation.get("score"));
            rows.add(row);
        }
        return rows;
    }

    private List<Object> rules(List<Object> basis, Map<String, Object> detailsByClause) {
        List<Object> rows = new ArrayList<>();
        for (Object item : basis) {
            Map<String, Object> citation = map(item);
            if (!"external".equals(stringValue(citation.get("corpus_type")))) {
                continue;
            }
            Map<String, Object> row = new HashMap<>();
            row.put("doc_id", valueOr(citation.get("source_doc_id"), citation.get("doc_title")));
            row.put("clause_id", citation.get("clause_id"));
            row.put("title", citation.get("doc_title"));
            row.put("issuer", "");
            row.put("doc_no", citation.get("source_code"));
            row.put("core_requirement", citation.get("clause_path"));
            Map<String, Object> detail = map(detailsByClause.get(stringValue(citation.get("clause_id"))));
            row.put("full_text", detail.get("text"));
            row.put("theme", "外部法规");
            rows.add(row);
        }
        return rows;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> sourceDetailsByClause(Object value) {
        Map<String, Object> result = new HashMap<>();
        for (Object item : list(value)) {
            Map<String, Object> detail = map(item);
            String clauseId = stringValue(detail.get("clause_id"));
            if (!clauseId.isEmpty()) result.put(clauseId, detail);
        }
        return result;
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

    private String stringValue(Object value) {
        return value instanceof String ? (String) value : "";
    }
}
