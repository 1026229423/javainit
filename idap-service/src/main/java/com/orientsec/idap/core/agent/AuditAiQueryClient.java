package com.orientsec.idap.core.agent;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONNull;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Java-to-audit-ai boundary client. It calls the stateless JSON endpoint and
 * never forwards browser authentication tokens to Python.
 */
@Component
@Slf4j
public class AuditAiQueryClient {
    private static final long DEFAULT_SOURCE_REFERENCE_TTL_MS = 10 * 60 * 1000L;
    private final ConcurrentMap<String, SourceReference> clauseSources = new ConcurrentHashMap<>();

    @Value("${audit.ai.query.enabled:false}")
    private boolean enabled;

    @Value("${audit.ai.query.base-url:}")
    private String baseUrl;

    @Value("${audit.ai.query.internal-token:}")
    private String internalToken;

    @Value("${audit.ai.query.permission-tags:}")
    private String permissionTags;

    @Value("${audit.ai.query.corpus-types:internal,external,qa,case}")
    private String corpusTypes;

    @Value("${audit.ai.query.connect-timeout-ms:5000}")
    private int connectTimeoutMs;

    @Value("${audit.ai.query.read-timeout-ms:60000}")
    private int readTimeoutMs;

    @Value("${audit.ai.query.source-reference-ttl-ms:600000}")
    private long sourceReferenceTtlMs = DEFAULT_SOURCE_REFERENCE_TTL_MS;

    public Map<String, Object> query(RegulationQueryRequest request, String queryId) throws QueryException {
        validateConfiguration();

        HttpURLConnection connection = null;
        try {
            connection = openConnection(request, queryId);
            int status = connection.getResponseCode();
            if (status < 200 || status >= 300) {
                log.warn("audit-ai query rejected, status={}, queryId={}", status, queryId);
                throw new QueryException("AUDIT_AI_QUERY_UPSTREAM_ERROR", "制度查询服务暂时不可用。");
            }
            Map<String, Object> response = parseResponse(connection.getInputStream());
            cacheDmClauseSources(response);
            return response;
        } catch (QueryException e) {
            throw e;
        } catch (IOException e) {
            log.warn("audit-ai query transport failed, queryId={}", queryId, e);
            throw new QueryException("AUDIT_AI_QUERY_TRANSPORT_ERROR", "制度查询连接失败，请稍后重试。");
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    /**
     * Loads the authoritative detail for a case returned by the query result.
     * The browser never calls audit-ai directly: the permission scope remains
     * a Java-side configuration and is sent to audit-ai through the internal
     * boundary only.
     */
    public Map<String, Object> queryCaseDetail(String caseId) throws QueryException {
        validateConfiguration();
        if (isBlank(caseId)) {
            throw new QueryException("REGULATION_CASE_INVALID_ID", "案例标识不能为空。");
        }

        HttpURLConnection connection = null;
        try {
            connection = openCaseDetailConnection(caseId);
            int status = connection.getResponseCode();
            if (status == HttpURLConnection.HTTP_NOT_FOUND) {
                throw new QueryException("REGULATION_CASE_NOT_FOUND", "案例不存在。");
            }
            if (status < 200 || status >= 300) {
                log.warn("audit-ai case detail rejected, status={}, caseId={}", status, caseId);
                throw new QueryException("AUDIT_AI_CASE_DETAIL_UPSTREAM_ERROR", "案例详情服务暂时不可用。");
            }
            return parseResponse(connection.getInputStream());
        } catch (QueryException e) {
            throw e;
        } catch (IOException e) {
            log.warn("audit-ai case detail transport failed, caseId={}", caseId, e);
            throw new QueryException("AUDIT_AI_CASE_DETAIL_TRANSPORT_ERROR", "案例详情连接失败，请稍后重试。");
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    /**
     * Loads full authoritative clause text from the source DM database. The
     * browser only provides a chunk id; its DM keys were cached from the same
     * permitted query response and are never accepted from the browser.
     */
    public Map<String, Object> queryClauseDetail(String clauseId) throws QueryException {
        validateConfiguration();
        if (isBlank(clauseId)) {
            throw new QueryException("REGULATION_CLAUSE_INVALID_ID", "条款标识不能为空。");
        }
        SourceReference sourceReference = getSourceReference(clauseId);
        if (sourceReference == null) {
            throw new QueryException("REGULATION_CLAUSE_SOURCE_NOT_FOUND", "条款回查标识已失效，请重新查询。");
        }

        HttpURLConnection connection = null;
        try {
            connection = openDmClauseDetailConnection(sourceReference);
            int status = connection.getResponseCode();
            if (status == HttpURLConnection.HTTP_NOT_FOUND) {
                throw new QueryException("REGULATION_CLAUSE_NOT_FOUND", "条款不存在。");
            }
            if (status < 200 || status >= 300) {
                log.warn("audit-ai DM clause detail rejected, status={}, clauseId={}", status, clauseId);
                throw new QueryException("AUDIT_AI_CLAUSE_DETAIL_UPSTREAM_ERROR", "条款详情服务暂时不可用。");
            }
            return parseResponse(connection.getInputStream());
        } catch (QueryException e) {
            throw e;
        } catch (IOException e) {
            log.warn("audit-ai DM clause detail transport failed, clauseId={}", clauseId, e);
            throw new QueryException("AUDIT_AI_CLAUSE_DETAIL_TRANSPORT_ERROR", "条款详情连接失败，请稍后重试。");
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private void validateConfiguration() throws QueryException {
        if (!enabled) {
            throw new QueryException("AUDIT_AI_QUERY_DISABLED", "制度查询服务尚未启用。");
        }
        if (isBlank(baseUrl) || isBlank(internalToken)) {
            throw new QueryException("AUDIT_AI_QUERY_NOT_CONFIGURED", "制度查询服务配置不完整。");
        }
        if (splitCsv(permissionTags).isEmpty()) {
            throw new QueryException("AUDIT_AI_QUERY_SCOPE_NOT_CONFIGURED", "制度查询权限范围尚未配置。");
        }
    }

    private HttpURLConnection openConnection(RegulationQueryRequest request, String queryId) throws IOException {
        URL url = new URL(trimTrailingSlash(baseUrl) + "/v1/query");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setConnectTimeout(connectTimeoutMs);
        connection.setReadTimeout(readTimeoutMs);
        connection.setDoOutput(true);
        connection.setRequestProperty("Accept", "application/json");
        connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
        connection.setRequestProperty("X-Internal-Token", internalToken);

        byte[] body = JSONUtil.toJsonStr(boundaryRequest(request, queryId)).getBytes(StandardCharsets.UTF_8);
        OutputStream output = connection.getOutputStream();
        try {
            output.write(body);
        } finally {
            output.close();
        }
        return connection;
    }

    private HttpURLConnection openCaseDetailConnection(String caseId) throws IOException {
        URL url = new URL(trimTrailingSlash(baseUrl) + "/v1/cases/" + URLEncoder.encode(caseId, "UTF-8"));
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setConnectTimeout(connectTimeoutMs);
        connection.setReadTimeout(readTimeoutMs);
        connection.setDoOutput(true);
        connection.setRequestProperty("Accept", "application/json");
        connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
        connection.setRequestProperty("X-Internal-Token", internalToken);

        Map<String, Object> filters = new HashMap<>();
        filters.put("perm_tags", splitCsv(permissionTags));
        Map<String, Object> body = new HashMap<>();
        body.put("filters", filters);

        OutputStream output = connection.getOutputStream();
        try {
            output.write(JSONUtil.toJsonStr(body).getBytes(StandardCharsets.UTF_8));
        } finally {
            output.close();
        }
        return connection;
    }

    private HttpURLConnection openDmClauseDetailConnection(SourceReference sourceReference) throws IOException {
        URL url = new URL(trimTrailingSlash(baseUrl) + "/v1/dm/clauses/"
                + URLEncoder.encode(sourceReference.getSourceCode(), "UTF-8"));
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setConnectTimeout(connectTimeoutMs);
        connection.setReadTimeout(readTimeoutMs);
        connection.setDoOutput(true);
        connection.setRequestProperty("Accept", "application/json");
        connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
        connection.setRequestProperty("X-Internal-Token", internalToken);

        Map<String, Object> body = new HashMap<>();
        body.put("source_doc_id", sourceReference.getSourceDocId());

        OutputStream output = connection.getOutputStream();
        try {
            output.write(JSONUtil.toJsonStr(body).getBytes(StandardCharsets.UTF_8));
        } finally {
            output.close();
        }
        return connection;
    }

    @SuppressWarnings("unchecked")
    private void cacheDmClauseSources(Map<String, Object> response) {
        cacheDmClauseSources(response.get("citations"));
        Object structured = response.get("structured");
        if (!(structured instanceof Map)) {
            return;
        }
        for (Object tab : ((Map<Object, Object>) structured).values()) {
            if (tab instanceof Map) {
                cacheDmClauseSources(((Map<Object, Object>) tab).get("items"));
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void cacheDmClauseSources(Object citations) {
        if (!(citations instanceof List)) {
            return;
        }
        long expiresAt = System.currentTimeMillis() + Math.max(sourceReferenceTtlMs, 1L);
        for (Object citation : (List<Object>) citations) {
            if (!(citation instanceof Map)) {
                continue;
            }
            Map<Object, Object> values = (Map<Object, Object>) citation;
            String clauseId = asNonBlankString(values.get("clause_id"));
            String sourceCode = asNonBlankString(values.get("source_code"));
            String sourceDocId = asNonBlankString(values.get("source_doc_id"));
            if (clauseId != null && sourceCode != null && sourceDocId != null) {
                clauseSources.put(clauseId, new SourceReference(sourceCode, sourceDocId, expiresAt));
            }
        }
    }

    private SourceReference getSourceReference(String clauseId) {
        SourceReference reference = clauseSources.get(clauseId);
        if (reference != null && reference.getExpiresAt() <= System.currentTimeMillis()) {
            clauseSources.remove(clauseId, reference);
            reference = null;
        }
        return reference;
    }

    private String asNonBlankString(Object value) {
        if (value == null) {
            return null;
        }
        String text = String.valueOf(value).trim();
        return text.isEmpty() ? null : text;
    }

    private Map<String, Object> boundaryRequest(RegulationQueryRequest request, String queryId) {
        Map<String, Object> filters = new HashMap<>();
        filters.put("perm_tags", splitCsv(permissionTags));
        filters.put("corpus_types", splitCsv(corpusTypes));

        Map<String, Object> options = new HashMap<>();
        options.put("include_superseded", request.getOptions().isIncludeSuperseded());

        Map<String, Object> body = new HashMap<>();
        body.put("query", request.getQuestion().trim());
        body.put("request_id", queryId);
        body.put("filters", filters);
        body.put("options", options);
        return body;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseResponse(InputStream input) throws IOException, QueryException {
        String raw = readBody(input);
        try {
            Object parsed = toPlainValue(JSONUtil.parse(raw));
            if (!(parsed instanceof Map)) {
                throw new QueryException("AUDIT_AI_QUERY_INVALID_RESPONSE", "制度查询服务返回格式错误。");
            }
            return (Map<String, Object>) parsed;
        } catch (QueryException e) {
            throw e;
        } catch (Exception e) {
            log.warn("audit-ai emitted malformed JSON response", e);
            throw new QueryException("AUDIT_AI_QUERY_INVALID_RESPONSE", "制度查询服务返回格式错误。");
        }
    }

    private String readBody(InputStream input) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8));
        StringBuilder body = new StringBuilder();
        char[] buffer = new char[4096];
        int count;
        while ((count = reader.read(buffer)) != -1) {
            body.append(buffer, 0, count);
        }
        return body.toString();
    }

    private Object toPlainValue(Object value) {
        if (value == null || value instanceof JSONNull) {
            return null;
        }
        if (value instanceof JSONObject) {
            Map<String, Object> output = new LinkedHashMap<>();
            for (Map.Entry<String, Object> entry : ((JSONObject) value).entrySet()) {
                output.put(entry.getKey(), toPlainValue(entry.getValue()));
            }
            return output;
        }
        if (value instanceof JSONArray) {
            List<Object> output = new ArrayList<>();
            for (Object item : (JSONArray) value) {
                output.add(toPlainValue(item));
            }
            return output;
        }
        return value;
    }

    private List<String> splitCsv(String value) {
        if (isBlank(value)) {
            return Collections.emptyList();
        }
        List<String> items = new ArrayList<>();
        for (String item : Arrays.asList(value.split(","))) {
            if (!isBlank(item)) {
                items.add(item.trim());
            }
        }
        return items;
    }

    private String trimTrailingSlash(String value) {
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    public static class QueryException extends Exception {
        private final String code;

        QueryException(String code, String message) {
            super(message);
            this.code = code;
        }

        public String getCode() {
            return code;
        }
    }

    private static class SourceReference {
        private final String sourceCode;
        private final String sourceDocId;
        private final long expiresAt;

        private SourceReference(String sourceCode, String sourceDocId, long expiresAt) {
            this.sourceCode = sourceCode;
            this.sourceDocId = sourceDocId;
            this.expiresAt = expiresAt;
        }

        private String getSourceCode() {
            return sourceCode;
        }

        private String getSourceDocId() {
            return sourceDocId;
        }

        private long getExpiresAt() {
            return expiresAt;
        }
    }
}
