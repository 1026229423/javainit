package com.orientsec.idap.core.agent;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;

import java.io.OutputStream;
import java.lang.reflect.Field;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TaskRuntimeQueryClientTest {

    @Test
    public void submitsPolicyQueryAndPollsUntilCompleted() throws Exception {
        AtomicReference<String> token = new AtomicReference<>();
        AtomicReference<String> submitBody = new AtomicReference<>();
        AtomicInteger polls = new AtomicInteger();
        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/runs", exchange -> {
            token.set(exchange.getRequestHeaders().getFirst("X-Internal-Token"));
            submitBody.set(read(exchange));
            reply(exchange, 202, "{\"runId\":\"run-1\",\"status\":\"running\"}");
        });
        server.createContext("/runs/run-1", exchange -> {
            if (polls.incrementAndGet() == 1) {
                reply(exchange, 200, "{\"runId\":\"run-1\",\"status\":\"running\"}");
                return;
            }
            reply(exchange, 200, "{\"runId\":\"run-1\",\"status\":\"completed\",\"answer\":{"
                    + "\"conclusion\":\"应当留存适当性材料。\",\"basis\":[{\"clause_id\":\"C-1\","
                    + "\"doc_title\":\"证券公司监督管理条例\",\"clause_path\":\"第十条\","
                    + "\"corpus_type\":\"external\",\"source_doc_id\":\"DOC-1\",\"score\":0.8}],"
                    + "\"source_details\":[{\"clause_id\":\"C-1\",\"text\":\"第十条原文\"}],"
                    + "\"confidence\":\"high\",\"finish_reason\":\"stop\"}}");
        });
        server.start();
        try {
            RegulationQueryRequest request = new RegulationQueryRequest();
            request.setQuestion("客户适当性依据");

            Map<String, Object> result = configuredClient(server).query(request, "request-1");

            assertEquals("test-internal-token", token.get());
            assertTrue(submitBody.get().contains("\"taskKind\":\"policy-query\""));
            assertTrue(submitBody.get().contains("\"clientRequestId\":\"request-1\""));
            assertTrue(submitBody.get().contains("\"permTags\":[\"internal\"]"));
            assertEquals("completed", result.get("status"));
            assertEquals(2, polls.get());
            Map<String, Object> browser = new AuditAiBoundaryResponseAdapter("session-1", "request-1", request.getQuestion())
                    .success(result);
            assertEquals("应当留存适当性材料。", ((Map) ((List) browser.get("answer_blocks")).get(0)).get("content"));
            assertEquals("C-1", ((Map) ((List) browser.get("citations")).get(0)).get("clause_id"));
            Map<String, Object> resultView = (Map<String, Object>) browser.get("result");
            assertEquals(0, ((Map) resultView.get("counts")).get("regulations"));
            assertEquals(0, ((Map) resultView.get("counts")).get("clauses"));
            assertEquals(1, ((Map) resultView.get("counts")).get("rules"));
            assertEquals("证券公司监督管理条例", ((Map) ((List) resultView.get("rules")).get(0)).get("title"));
            assertEquals("C-1", ((Map) ((List) resultView.get("rules")).get(0)).get("clause_id"));
            assertEquals("第十条原文", ((Map) ((List) resultView.get("rules")).get(0)).get("full_text"));
        } finally {
            server.stop(0);
        }
    }

    @Test
    public void rejectsTerminalFailuresWithoutReadingAnswer() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/runs", exchange -> reply(exchange, 200,
                "{\"runId\":\"run-1\",\"status\":\"error\",\"output\":\"{bad}\"}"));
        server.start();
        try {
            RegulationQueryRequest request = new RegulationQueryRequest();
            request.setQuestion("客户适当性依据");

            TaskRuntimeQueryClient.QueryException error = assertThrows(TaskRuntimeQueryClient.QueryException.class,
                    () -> configuredClient(server).query(request, "request-1"));

            assertEquals("TASK_RUNTIME_QUERY_ERROR", error.getCode());
        } finally {
            server.stop(0);
        }
    }

    @Test
    public void submitsClauseDetailAsDedicatedTaskKind() throws Exception {
        AtomicReference<String> submitBody = new AtomicReference<>();
        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/runs", exchange -> {
            submitBody.set(read(exchange));
            reply(exchange, 200, "{\"runId\":\"detail-1\",\"status\":\"completed\",\"answer\":{"
                    + "\"clause_id\":\"C-1\",\"found\":true,\"text\":\"条款原文\"}}");
        });
        server.start();
        try {
            Map<String, Object> citation = new HashMap<>();
            citation.put("clause_id", "C-1");
            citation.put("doc_title", "证券公司监督管理条例");
            citation.put("clause_path", "第十条");
            citation.put("corpus_type", "external");

            Map<String, Object> result = configuredClient(server).queryClauseDetail(citation, "detail-request-1");

            assertEquals("completed", result.get("status"));
            assertTrue(submitBody.get().contains("\"taskKind\":\"policy-clause-detail\""));
            assertTrue(submitBody.get().contains("\"corpusTypes\":[\"internal\"]"));
            assertTrue(submitBody.get().contains("\\\"clause_id\\\":\\\"C-1\\\""));
        } finally {
            server.stop(0);
        }
    }

    private TaskRuntimeQueryClient configuredClient(HttpServer server) throws Exception {
        TaskRuntimeQueryClient client = new TaskRuntimeQueryClient();
        setField(client, "enabled", true);
        setField(client, "host", "127.0.0.1");
        setField(client, "port", server.getAddress().getPort());
        setField(client, "internalToken", "test-internal-token");
        setField(client, "permissionTags", "internal");
        setField(client, "corpusTypes", "internal");
        setField(client, "connectTimeoutMs", 1000);
        setField(client, "readTimeoutMs", 1000);
        setField(client, "pollIntervalMs", 1);
        setField(client, "pollTimeoutMs", 1000L);
        return client;
    }

    private static String read(com.sun.net.httpserver.HttpExchange exchange) {
        java.util.Scanner scanner = new java.util.Scanner(exchange.getRequestBody(), "UTF-8").useDelimiter("\\A");
        return scanner.hasNext() ? scanner.next() : "";
    }

    private static void reply(com.sun.net.httpserver.HttpExchange exchange, int status, String body) throws java.io.IOException {
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, body.getBytes("UTF-8").length);
        OutputStream output = exchange.getResponseBody();
        output.write(body.getBytes("UTF-8"));
        output.close();
    }

    private void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}
