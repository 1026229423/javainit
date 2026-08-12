package com.orientsec.idap.core.policycompare;

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
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** HTTP client for task-runtime's deterministic policy-compare coverage workflow. */
@Component
@Slf4j
public class TaskRuntimePolicyCompareClient {
    @Value("${task.runtime.compare.enabled:false}")
    private boolean enabled;
    @Value("${task.runtime.compare.host:127.0.0.1}")
    private String host;
    @Value("${task.runtime.compare.port:0}")
    private int port;
    @Value("${task.runtime.compare.internal-token:}")
    private String internalToken;
    @Value("${task.runtime.compare.permission-tags:}")
    private String permissionTags;
    @Value("${task.runtime.compare.corpus-types:internal}")
    private String corpusTypes;
    @Value("${task.runtime.compare.connect-timeout-ms:5000}")
    private int connectTimeoutMs;
    @Value("${task.runtime.compare.read-timeout-ms:60000}")
    private int readTimeoutMs;

    public Map<String, Object> submitCoverage(PolicyCompareTaskCreateRequest request, String requestId)
            throws CompareException {
        validateConfiguration();
        String subjectType = isBlank(request.getCoverageSubjectType())
                ? PolicyCompareTaskService.COVERAGE_SUBJECT_EXTERNAL : request.getCoverageSubjectType().trim();
        String direction = PolicyCompareTaskService.COVERAGE_SUBJECT_INTERNAL.equals(subjectType)
                ? "internal_to_external" : "external_to_internal";
        Map<String, Object> subject = new HashMap<>();
        if (isBlank(request.getPrimaryObjectSource())
                || PolicyCompareTaskService.PRIMARY_SOURCE_LIBRARY.equals(request.getPrimaryObjectSource().trim())) {
            subject.put("source", "library");
            subject.put("docVersionId", request.getPrimaryDocVersionId().trim());
        } else {
            PolicyCompareTaskCreateRequest.ExternalArtifact artifact = request.getExternalArtifact();
            subject.put("source", "upload");
            subject.put("objectKey", artifact.getObjectKey().trim());
            subject.put("uploadId", artifact.getUploadId().trim());
            subject.put("filename", artifact.getFilename().trim());
        }
        Map<String, Object> scope = new HashMap<>();
        if (request.getScope() != null && request.getScope().getEffectiveDateRange() != null
                && !request.getScope().getEffectiveDateRange().isEmpty()) {
            scope.put("effectiveDateRange", request.getScope().getEffectiveDateRange());
        }
        Map<String, Object> payload = new HashMap<>();
        payload.put("direction", direction);
        payload.put(PolicyCompareTaskService.COVERAGE_SUBJECT_INTERNAL.equals(subjectType) ? "internal" : "external", subject);
        payload.put("scope", scope);

        Map<String, Object> filters = new HashMap<>();
        filters.put("permTags", splitCsv(permissionTags));
        filters.put("corpusTypes", splitCsv(corpusTypes));
        filters.put("projectId", null);
        filters.put("owner", null);

        Map<String, Object> body = new HashMap<>();
        body.put("taskKind", "policy-compare-coverage");
        body.put("input", PolicyCompareTaskService.COVERAGE_SUBJECT_INTERNAL.equals(subjectType)
                ? "核查指定内规引用的外规是否已有版本变动" : "对指定外规执行内部制度覆盖度核查");
        body.put("clientRequestId", requestId);
        body.put("requestId", requestId);
        body.put("filters", filters);
        body.put("payload", payload);
        body.put("waitMs", 30000);
        return request("POST", "/runs", body);
    }

    /** Submits two selected versions of one logical internal policy or external regulation. */
    public Map<String, Object> submitVersionDiff(PolicyCompareTaskCreateRequest request, String requestId)
            throws CompareException {
        validateConfiguration();
        Map<String, Object> payload = new HashMap<>();
        payload.put("newDocVersionId", request.getPrimaryDocVersionId().trim());
        payload.put("oldDocVersionId", request.getSecondaryDocVersionId().trim());

        Map<String, Object> filters = new HashMap<>();
        filters.put("permTags", splitCsv(permissionTags));
        filters.put("corpusTypes", splitCsv(corpusTypes));
        filters.put("projectId", null);
        filters.put("owner", null);

        Map<String, Object> body = new HashMap<>();
        body.put("taskKind", "policy-compare-version-diff");
        body.put("input", "对同一制度或规则的两个不同版本执行条款差异比对");
        body.put("clientRequestId", requestId);
        body.put("requestId", requestId);
        body.put("filters", filters);
        body.put("payload", payload);
        body.put("waitMs", 30000);
        return request("POST", "/runs", body);
    }

    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> listExternalDocuments() throws CompareException {
        return listDocuments("/library/external-documents", true);
    }

    public List<Map<String, Object>> listInternalDocuments() throws CompareException {
        return listDocuments("/library/internal-documents", true);
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> listDocuments(String endpoint, boolean includeHistory) throws CompareException {
        validateConfiguration();
        StringBuilder path = new StringBuilder(endpoint);
        boolean first = true;
        for (String tag : splitCsv(permissionTags)) {
            path.append(first ? '?' : '&').append("permTag=").append(urlEncode(tag));
            first = false;
        }
        if (includeHistory) path.append(first ? '?' : '&').append("includeHistory=true");
        Object items = requestObject("GET", path.toString(), null);
        if (!(items instanceof List)) {
            throw new CompareException("TASK_RUNTIME_COMPARE_INVALID_RESPONSE", "制度库目录返回格式错误。");
        }
        return (List<Map<String, Object>>) items;
    }

    public Map<String, Object> findRun(String runId) throws CompareException {
        validateConfiguration();
        if (isBlank(runId)) throw new CompareException("TASK_RUNTIME_COMPARE_INVALID_REQUEST", "比对运行标识不能为空。");
        return request("GET", "/runs/" + runId, null);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> request(String method, String path, Map<String, Object> body) throws CompareException {
        Object parsed = requestObject(method, path, body);
        if (!(parsed instanceof Map)) {
            throw new CompareException("TASK_RUNTIME_COMPARE_INVALID_RESPONSE", "制度比对服务返回格式错误。");
        }
        return (Map<String, Object>) parsed;
    }

    private Object requestObject(String method, String path, Map<String, Object> body) throws CompareException {
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) new URL("http://" + host + ":" + port + path).openConnection();
            connection.setRequestMethod(method);
            connection.setConnectTimeout(connectTimeoutMs);
            connection.setReadTimeout(readTimeoutMs);
            connection.setRequestProperty("Accept", "application/json");
            connection.setRequestProperty("X-Internal-Token", internalToken);
            if (body != null) {
                connection.setDoOutput(true);
                connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
                try (OutputStream output = connection.getOutputStream()) {
                    output.write(JSONUtil.toJsonStr(body).getBytes(StandardCharsets.UTF_8));
                }
            }
            int status = connection.getResponseCode();
            InputStream input = status >= 200 && status < 300 ? connection.getInputStream() : connection.getErrorStream();
            String responseBody = readBody(input);
            if (status < 200 || status >= 300) {
                String code = status == 401 ? "TASK_RUNTIME_COMPARE_UNAUTHORIZED" : "TASK_RUNTIME_COMPARE_UPSTREAM_ERROR";
                throw new CompareException(code, upstreamMessage(responseBody, "制度比对服务暂时不可用。"));
            }
            Object parsed = JSONUtil.parse(responseBody);
            if (parsed instanceof cn.hutool.json.JSONObject) {
                return ((cn.hutool.json.JSONObject) parsed).toBean(Map.class);
            }
            if (parsed instanceof cn.hutool.json.JSONArray) {
                return ((cn.hutool.json.JSONArray) parsed).toList(Map.class);
            }
            throw new CompareException("TASK_RUNTIME_COMPARE_INVALID_RESPONSE", "制度比对服务返回格式错误。");
        } catch (CompareException e) {
            throw e;
        } catch (IOException | RuntimeException e) {
            log.warn("task-runtime policy compare transport failed", e);
            throw new CompareException("TASK_RUNTIME_COMPARE_TRANSPORT_ERROR", "制度比对服务连接失败，请稍后重试。");
        } finally {
            if (connection != null) connection.disconnect();
        }
    }

    private String urlEncode(String value) throws CompareException {
        try {
            return URLEncoder.encode(value, "UTF-8");
        } catch (java.io.UnsupportedEncodingException e) {
            throw new CompareException("TASK_RUNTIME_COMPARE_INVALID_REQUEST", "制度库权限标签编码失败。");
        }
    }

    private void validateConfiguration() throws CompareException {
        if (!enabled) throw new CompareException("TASK_RUNTIME_COMPARE_DISABLED", "制度比对服务尚未启用。");
        if (isBlank(host) || port <= 0 || isBlank(internalToken)) {
            throw new CompareException("TASK_RUNTIME_COMPARE_NOT_CONFIGURED", "制度比对服务配置不完整。");
        }
        if (splitCsv(corpusTypes).isEmpty()) {
            throw new CompareException("TASK_RUNTIME_COMPARE_SCOPE_NOT_CONFIGURED", "制度比对权限范围尚未配置。");
        }
    }

    @SuppressWarnings("unchecked")
    private String upstreamMessage(String responseBody, String fallback) {
        try {
            Object parsed = JSONUtil.parse(responseBody);
            if (parsed instanceof cn.hutool.json.JSONObject) {
                Map<String, Object> map = ((cn.hutool.json.JSONObject) parsed).toBean(Map.class);
                Object message = map.get("message");
                if (message != null && !String.valueOf(message).trim().isEmpty()) return String.valueOf(message);
                Object error = map.get("error");
                if (error instanceof Map && ((Map<?, ?>) error).get("message") != null) {
                    return String.valueOf(((Map<?, ?>) error).get("message"));
                }
            }
        } catch (RuntimeException ignored) {
            // Keep the stable browser-facing fallback when the upstream body is not JSON.
        }
        return fallback;
    }

    private String readBody(InputStream input) throws IOException {
        if (input == null) return "";
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
            StringBuilder body = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) body.append(line);
            return body.toString();
        }
    }

    private List<String> splitCsv(String value) {
        return isBlank(value) ? Collections.<String>emptyList() : Arrays.asList(value.split("\\s*,\\s*"));
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    public static class CompareException extends Exception {
        private final String code;

        public CompareException(String code, String message) {
            super(message);
            this.code = code;
        }

        public String getCode() {
            return code;
        }
    }
}
