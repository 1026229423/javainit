package com.orientsec.idap.core.policycompare;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;

import java.io.OutputStream;
import java.lang.reflect.Field;
import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TaskRuntimePolicyCompareClientTest {

    @Test
    public void submitsCoverageContractAndReadsProgressThenStructuredAnswer() throws Exception {
        AtomicReference<String> submitBody = new AtomicReference<>();
        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/runs", exchange -> {
            submitBody.set(read(exchange));
            reply(exchange, 202, "{\"runId\":\"compare-1\",\"status\":\"running\"}");
        });
        server.createContext("/runs/compare-1", exchange -> reply(exchange, 200,
                "{\"runId\":\"compare-1\",\"status\":\"running\",\"progress\":{"
                        + "\"stage\":\"matching\",\"percent\":55,\"current\":2,\"total\":4,"
                        + "\"message\":\"正在匹配内部制度条款\"}}"));
        server.start();
        try {
            PolicyCompareTaskCreateRequest request = coverageRequest();
            TaskRuntimePolicyCompareClient client = configuredClient(server);

            Map<String, Object> submitted = client.submitCoverage(request, "request-1");
            Map<String, Object> progress = client.findRun("compare-1");

            assertEquals("running", submitted.get("status"));
            assertEquals("matching", ((Map) progress.get("progress")).get("stage"));
            assertTrue(submitBody.get().contains("\"taskKind\":\"policy-compare-coverage\""));
            assertTrue(submitBody.get().contains("\"objectKey\":\"upload/U1/policy.pdf\""));
            assertTrue(submitBody.get().contains("\"uploadId\":\"U1\""));
            assertTrue(submitBody.get().contains("\"filename\":\"policy.pdf\""));
            assertTrue(submitBody.get().contains("\"effectiveDateRange\":[\"2024-01-01\",\"2026-12-31\"]"));
            assertTrue(submitBody.get().contains("\"corpusTypes\":[\"internal\"]"));
        } finally {
            server.stop(0);
        }
    }

    @Test
    public void listsExternalDocumentsAndSubmitsSelectedLibraryVersion() throws Exception {
        AtomicReference<String> catalogQuery = new AtomicReference<>();
        AtomicReference<String> submitBody = new AtomicReference<>();
        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/library/external-documents", exchange -> {
            catalogQuery.set(exchange.getRequestURI().getRawQuery());
            assertEquals("test-token", exchange.getRequestHeaders().getFirst("X-Internal-Token"));
            reply(exchange, 200, "[{\"logicalId\":\"LAW-1\",\"docVersionId\":\"DV-2026\","
                    + "\"title\":\"上市公司监管办法\",\"versionLabel\":\"2026版\"}]");
        });
        server.createContext("/runs", exchange -> {
            submitBody.set(read(exchange));
            reply(exchange, 202, "{\"runId\":\"compare-library-1\",\"status\":\"queued\"}");
        });
        server.start();
        try {
            TaskRuntimePolicyCompareClient client = configuredClient(server);
            List<Map<String, Object>> catalog = client.listExternalDocuments();

            PolicyCompareTaskCreateRequest request = new PolicyCompareTaskCreateRequest();
            request.setMode(PolicyCompareTaskService.INTERNAL_COVERAGE);
            request.setPrimaryObjectSource(PolicyCompareTaskService.PRIMARY_SOURCE_LIBRARY);
            request.setPrimaryObject("上市公司监管办法 · 2026版");
            request.setPrimaryDocVersionId("DV-2026");
            request.setSecondaryObject("内部制度库");
            Map<String, Object> submitted = client.submitCoverage(request, "request-library-1");

            assertEquals(1, catalog.size());
            assertEquals("DV-2026", catalog.get(0).get("docVersionId"));
            assertTrue(catalogQuery.get().contains("permTag=internal"));
            assertEquals("queued", submitted.get("status"));
            assertTrue(submitBody.get().contains("\"source\":\"library\""));
            assertTrue(submitBody.get().contains("\"docVersionId\":\"DV-2026\""));
            assertTrue(!submitBody.get().contains("\"objectKey\""));
        } finally {
            server.stop(0);
        }
    }

    @Test
    public void listsInternalDocumentsAndSubmitsReverseReferenceVersionCheck() throws Exception {
        AtomicReference<String> submitBody = new AtomicReference<>();
        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/library/internal-documents", exchange -> reply(exchange, 200,
                "[{\"logicalId\":\"INT-1\",\"docVersionId\":\"INT-DV-1\","
                        + "\"title\":\"费用报销管理办法\",\"versionLabel\":\"2026版\"}]"));
        server.createContext("/runs", exchange -> {
            submitBody.set(read(exchange));
            reply(exchange, 202, "{\"runId\":\"reverse-1\",\"status\":\"queued\"}");
        });
        server.start();
        try {
            TaskRuntimePolicyCompareClient client = configuredClient(server);
            List<Map<String, Object>> catalog = client.listInternalDocuments();
            PolicyCompareTaskCreateRequest request = new PolicyCompareTaskCreateRequest();
            request.setMode(PolicyCompareTaskService.INTERNAL_COVERAGE);
            request.setCoverageSubjectType(PolicyCompareTaskService.COVERAGE_SUBJECT_INTERNAL);
            request.setPrimaryObjectSource(PolicyCompareTaskService.PRIMARY_SOURCE_LIBRARY);
            request.setPrimaryObject("费用报销管理办法 · 2026版");
            request.setPrimaryDocVersionId("INT-DV-1");

            client.submitCoverage(request, "reverse-request-1");

            assertEquals("INT-DV-1", catalog.get(0).get("docVersionId"));
            assertTrue(submitBody.get().contains("\"direction\":\"internal_to_external\""));
            assertTrue(submitBody.get().contains("\"internal\":{") || submitBody.get().contains("\"internal\" : {"));
            assertTrue(submitBody.get().contains("\"docVersionId\":\"INT-DV-1\""));
            assertTrue(!submitBody.get().contains("\"external\":{"));
        } finally {
            server.stop(0);
        }
    }

    @Test
    public void submitsSameDocumentVersionDiffContract() throws Exception {
        AtomicReference<String> submitBody = new AtomicReference<>();
        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/runs", exchange -> {
            submitBody.set(read(exchange));
            reply(exchange, 202, "{\"runId\":\"version-diff-1\",\"status\":\"queued\"}");
        });
        server.start();
        try {
            TaskRuntimePolicyCompareClient client = configuredClient(server);
            PolicyCompareTaskCreateRequest request = new PolicyCompareTaskCreateRequest();
            request.setMode(PolicyCompareTaskService.EXTERNAL_VERSION);
            request.setPrimaryObjectSource(PolicyCompareTaskService.PRIMARY_SOURCE_LIBRARY);
            request.setPrimaryObject("上市公司信息披露管理办法 · 2026版");
            request.setPrimaryDocVersionId("DV-2026");
            request.setSecondaryObject("上市公司信息披露管理办法 · 2025版");
            request.setSecondaryDocVersionId("DV-2025");

            Map<String, Object> submitted = client.submitVersionDiff(request, "version-diff-request-1");

            assertEquals("queued", submitted.get("status"));
            assertTrue(submitBody.get().contains("\"taskKind\":\"policy-compare-version-diff\""));
            assertTrue(submitBody.get().contains("\"newDocVersionId\":\"DV-2026\""));
            assertTrue(submitBody.get().contains("\"oldDocVersionId\":\"DV-2025\""));
        } finally {
            server.stop(0);
        }
    }

    private PolicyCompareTaskCreateRequest coverageRequest() {
        PolicyCompareTaskCreateRequest request = new PolicyCompareTaskCreateRequest();
        request.setMode(PolicyCompareTaskService.INTERNAL_COVERAGE);
        request.setPrimaryObjectSource(PolicyCompareTaskService.PRIMARY_SOURCE_UPLOAD);
        request.setSecondaryObject("内部制度知识库");
        PolicyCompareTaskCreateRequest.ExternalArtifact artifact = new PolicyCompareTaskCreateRequest.ExternalArtifact();
        artifact.setObjectKey("upload/U1/policy.pdf");
        artifact.setUploadId("U1");
        artifact.setFilename("policy.pdf");
        request.setExternalArtifact(artifact);
        PolicyCompareTaskCreateRequest.Scope scope = new PolicyCompareTaskCreateRequest.Scope();
        scope.setEffectiveDateRange(Arrays.asList("2024-01-01", "2026-12-31"));
        request.setScope(scope);
        return request;
    }

    private TaskRuntimePolicyCompareClient configuredClient(HttpServer server) throws Exception {
        TaskRuntimePolicyCompareClient client = new TaskRuntimePolicyCompareClient();
        setField(client, "enabled", true);
        setField(client, "host", "127.0.0.1");
        setField(client, "port", server.getAddress().getPort());
        setField(client, "internalToken", "test-token");
        setField(client, "permissionTags", "internal");
        setField(client, "corpusTypes", "internal");
        setField(client, "connectTimeoutMs", 1000);
        setField(client, "readTimeoutMs", 1000);
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
