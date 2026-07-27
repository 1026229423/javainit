package com.orientsec.idap.core.agent;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;

import java.io.OutputStream;
import java.lang.reflect.Field;
import java.net.InetSocketAddress;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class AuditAiQueryClientTest {

    @Test
    public void forwardsConfiguredScopeAndAdaptsCompleteJsonResponse() throws Exception {
        final AtomicReference<String> accept = new AtomicReference<>();
        final AtomicReference<String> token = new AtomicReference<>();
        final AtomicReference<String> requestBody = new AtomicReference<>();
        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/v1/query", exchange -> {
            accept.set(exchange.getRequestHeaders().getFirst("Accept"));
            token.set(exchange.getRequestHeaders().getFirst("X-Internal-Token"));
            java.util.Scanner scanner = new java.util.Scanner(exchange.getRequestBody(), "UTF-8").useDelimiter("\\A");
            requestBody.set(scanner.hasNext() ? scanner.next() : "");
            String response = "{\"meta\":{\"request_id\":\"query-1\"},"
                    + "\"answer_blocks\":[{\"block_seq\":0,\"block_type\":\"text\",\"content\":\"已找到相关制度。\"}],"
                    + "\"citations\":[{\"clause_id\":\"chunk-1\",\"source_doc_id\":null}],"
                    + "\"structured\":{\"regulations\":{\"total\":1,\"items\":[{\"doc_id\":\"DOC-1\",\"title\":\"信息披露管理办法\"}]},"
                    + "\"clauses\":{\"total\":1,\"items\":[{\"clause_id\":\"chunk-1\"}]},"
                    + "\"regulatory_rules\":{\"total\":0,\"items\":[]},\"cases\":{\"total\":0,\"items\":[]},"
                    + "\"citation_advice\":[]},"
                    + "\"completion\":{\"finish_reason\":\"stop\",\"exhausted_scope\":[]}}";
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, response.getBytes("UTF-8").length);
            OutputStream output = exchange.getResponseBody();
            output.write(response.getBytes("UTF-8"));
            output.close();
        });
        server.start();
        try {
            AuditAiQueryClient client = configuredClient(server);
            RegulationQueryRequest request = new RegulationQueryRequest();
            request.setQuestion("客户风险等级更新不及时的相关制度有哪些？");

            Map<String, Object> upstream = client.query(request, "query-1");
            Map<String, Object> response = new AuditAiBoundaryResponseAdapter(
                    "session-1", "query-1", request.getQuestion()).success(upstream);

            assertEquals("application/json", accept.get());
            assertEquals("test-internal-token", token.get());
            assertTrue(requestBody.get().contains("audit-policy-read"));
            assertTrue(requestBody.get().contains("internal"));
            assertEquals("session-1", ((Map) response.get("context")).get("session_id"));
            assertEquals("已找到相关制度。", ((Map) ((List) response.get("answer_blocks")).get(0)).get("content"));
            assertNull(((Map) ((List) response.get("citations")).get(0)).get("source_doc_id"));
            Map result = (Map) response.get("result");
            assertEquals(1, ((Map) result.get("counts")).get("regulations"));
            assertEquals("信息披露管理办法", ((Map) ((List) result.get("regulations")).get(0)).get("title"));
            assertEquals(1, ((Map) result.get("counts")).get("clauses"));
            assertEquals("stop", ((Map) response.get("completion")).get("finish_reason"));
        } finally {
            server.stop(0);
        }
    }

    @Test
    public void createsCompleteErrorBodyWithoutStreamingEvents() {
        Map<String, Object> response = new AuditAiBoundaryResponseAdapter("session-1", "query-1", "查询问题")
                .error("AUDIT_AI_QUERY_DISABLED", "制度查询服务尚未启用。");

        Map error = (Map) response.get("error");
        assertEquals("AUDIT_AI_QUERY_DISABLED", error.get("code"));
        assertEquals("query-1", error.get("query_id"));
        assertEquals(Collections.singleton("error"), response.keySet());
    }

    @Test
    public void forwardsPermissionScopeWhenLoadingCaseDetail() throws Exception {
        final AtomicReference<String> requestBody = new AtomicReference<>();
        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/v1/cases/CASE-1", exchange -> {
            java.util.Scanner scanner = new java.util.Scanner(exchange.getRequestBody(), "UTF-8").useDelimiter("\\A");
            requestBody.set(scanner.hasNext() ? scanner.next() : "");
            String response = "{\"case_id\":\"CASE-1\",\"case_name\":\"信息披露违规案例\","
                    + "\"full_text\":\"案例完整正文\",\"source_url\":null}";
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, response.getBytes("UTF-8").length);
            OutputStream output = exchange.getResponseBody();
            output.write(response.getBytes("UTF-8"));
            output.close();
        });
        server.start();
        try {
            Map<String, Object> detail = configuredClient(server).queryCaseDetail("CASE-1");

            assertEquals("CASE-1", detail.get("case_id"));
            assertEquals("案例完整正文", detail.get("full_text"));
            assertTrue(requestBody.get().contains("audit-policy-read"));
        } finally {
            server.stop(0);
        }
    }

    private AuditAiQueryClient configuredClient(HttpServer server) throws Exception {
        AuditAiQueryClient client = new AuditAiQueryClient();
        setField(client, "enabled", true);
        setField(client, "baseUrl", "http://127.0.0.1:" + server.getAddress().getPort());
        setField(client, "internalToken", "test-internal-token");
        setField(client, "permissionTags", "audit-policy-read");
        setField(client, "corpusTypes", "internal,external");
        setField(client, "connectTimeoutMs", 1000);
        setField(client, "readTimeoutMs", 1000);
        return client;
    }

    private void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}
