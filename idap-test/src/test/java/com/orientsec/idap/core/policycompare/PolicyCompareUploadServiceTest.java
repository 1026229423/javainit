package com.orientsec.idap.core.policycompare;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class PolicyCompareUploadServiceTest {

    @Test
    public void uploadsOneDocumentThroughSignedS3PutAndReturnsArtifactReference() throws Exception {
        byte[] document = "policy-content".getBytes(StandardCharsets.UTF_8);
        AtomicReference<String> requestPath = new AtomicReference<>();
        AtomicReference<String> authorization = new AtomicReference<>();
        AtomicReference<byte[]> received = new AtomicReference<>();
        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/dfzq-uploads/", exchange -> {
            requestPath.set(exchange.getRequestURI().getRawPath());
            authorization.set(exchange.getRequestHeaders().getFirst("Authorization"));
            received.set(readAll(exchange.getRequestBody()));
            exchange.sendResponseHeaders(200, -1);
            exchange.close();
        });
        server.start();
        try {
            PolicyCompareUploadService service = configuredService(server);
            MockMultipartFile file = new MockMultipartFile(
                    "file", "外规 测试.pdf", "text/html", document);

            PolicyCompareUploadView result = service.upload(file);

            assertEquals("外规 测试.pdf", result.getFilename());
            assertEquals("application/pdf", result.getContentType());
            assertEquals(document.length, result.getSize());
            assertEquals(result.getFileId(), result.getUploadId());
            assertTrue(result.getObjectKey().startsWith("upload/" + result.getUploadId() + "/"));
            assertTrue(requestPath.get().startsWith("/dfzq-uploads/upload/"));
            assertTrue(requestPath.get().endsWith("/%E5%A4%96%E8%A7%84%20%E6%B5%8B%E8%AF%95.pdf"));
            assertNotNull(authorization.get());
            assertTrue(authorization.get().startsWith("AWS4-HMAC-SHA256 Credential=test-access/"));
            assertArrayEquals(document, received.get());
        } finally {
            server.stop(0);
        }
    }

    @Test
    public void rejectsLegacyOfficeAndSpreadsheetFormatsThatTheParserCannotProcess() throws Exception {
        PolicyCompareUploadService service = new PolicyCompareUploadService();
        setField(service, "enabled", true);
        setField(service, "endpoint", "http://127.0.0.1:1");
        setField(service, "bucket", "dfzq-uploads");
        setField(service, "accessKey", "test-access");
        setField(service, "secretKey", "test-secret");
        setField(service, "region", "us-east-1");
        setField(service, "maxBytes", 1024L);

        for (String filename : new String[] {"legacy.doc", "sheet.xls", "sheet.xlsx"}) {
            MockMultipartFile file = new MockMultipartFile(
                    "file", filename, "application/octet-stream", "content".getBytes(StandardCharsets.UTF_8));

            PolicyCompareUploadService.UploadException error = assertThrows(
                    PolicyCompareUploadService.UploadException.class,
                    () -> service.upload(file));

            assertEquals("POLICY_COMPARE_UPLOAD_UNSUPPORTED_TYPE", error.getCode());
            assertEquals("仅支持 PDF、DOCX 文件。", error.getMessage());
        }
    }

    private PolicyCompareUploadService configuredService(HttpServer server) throws Exception {
        PolicyCompareUploadService service = new PolicyCompareUploadService();
        setField(service, "enabled", true);
        setField(service, "endpoint", "http://127.0.0.1:" + server.getAddress().getPort());
        setField(service, "bucket", "dfzq-uploads");
        setField(service, "accessKey", "test-access");
        setField(service, "secretKey", "test-secret");
        setField(service, "region", "us-east-1");
        setField(service, "maxBytes", 1024L);
        return service;
    }

    private static byte[] readAll(InputStream input) throws java.io.IOException {
        try (InputStream in = input; ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[512];
            int read;
            while ((read = in.read(buffer)) >= 0) output.write(buffer, 0, read);
            return output.toByteArray();
        }
    }

    private void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}
