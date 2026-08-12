package com.orientsec.idap.core.policycompare;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/** Stores one comparison input in the shared MinIO bucket using the S3 REST contract. */
@Service
public class PolicyCompareUploadService {
    private static final Set<String> ALLOWED_EXTENSIONS = new HashSet<>(
            Arrays.asList("pdf", "docx"));
    private static final DateTimeFormatter AMZ_DATE = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'");
    private static final DateTimeFormatter DATE_STAMP = DateTimeFormatter.ofPattern("yyyyMMdd");

    @Value("${policy.compare.upload.enabled:false}")
    private boolean enabled;
    @Value("${policy.compare.upload.endpoint:}")
    private String endpoint;
    @Value("${policy.compare.upload.bucket:}")
    private String bucket;
    @Value("${policy.compare.upload.access-key:}")
    private String accessKey;
    @Value("${policy.compare.upload.secret-key:}")
    private String secretKey;
    @Value("${policy.compare.upload.region:us-east-1}")
    private String region;
    @Value("${policy.compare.upload.max-bytes:52428800}")
    private long maxBytes;

    public PolicyCompareUploadView upload(MultipartFile file) throws UploadException {
        validateConfiguration();
        if (file == null || file.isEmpty()) throw new UploadException("POLICY_COMPARE_UPLOAD_EMPTY", "请选择待比对文件。");
        if (file.getSize() > maxBytes) throw new UploadException("POLICY_COMPARE_UPLOAD_TOO_LARGE", "文件大小不能超过 50MB。");
        String filename = safeFilename(file.getOriginalFilename());
        String extension = extension(filename);
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new UploadException("POLICY_COMPARE_UPLOAD_UNSUPPORTED_TYPE", "仅支持 PDF、DOCX 文件。");
        }
        byte[] bytes;
        try {
            bytes = readAll(file.getInputStream(), maxBytes);
        } catch (IOException e) {
            throw new UploadException("POLICY_COMPARE_UPLOAD_READ_ERROR", "读取上传文件失败。");
        }
        String uploadId = UUID.randomUUID().toString();
        String objectKey = "upload/" + uploadId + "/" + filename;
        String contentType = contentType(extension);
        putObject(objectKey, contentType, bytes);

        PolicyCompareUploadView view = new PolicyCompareUploadView();
        view.setFileId(uploadId);
        view.setUploadId(uploadId);
        view.setObjectKey(objectKey);
        view.setFilename(filename);
        view.setContentType(contentType);
        view.setSize(bytes.length);
        return view;
    }

    private void putObject(String objectKey, String contentType, byte[] payload) throws UploadException {
        try {
            URL base = new URL(endpoint.replaceAll("/+$", ""));
            String canonicalUri = "/" + encodePath(bucket) + "/" + encodePath(objectKey);
            URL target = new URL(base.getProtocol(), base.getHost(), base.getPort(), canonicalUri);
            HttpURLConnection connection = (HttpURLConnection) target.openConnection();
            connection.setRequestMethod("PUT");
            connection.setDoOutput(true);
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(60000);
            connection.setFixedLengthStreamingMode(payload.length);
            sign(connection, base, "PUT", canonicalUri, contentType, payload);
            try (OutputStream output = connection.getOutputStream()) {
                output.write(payload);
            }
            int status = connection.getResponseCode();
            if (status < 200 || status >= 300) {
                String detail = readText(connection.getErrorStream());
                throw new UploadException("POLICY_COMPARE_UPLOAD_STORAGE_ERROR",
                        "文件写入对象存储失败（HTTP " + status + "）" + (detail.isEmpty() ? "。" : "：" + detail));
            }
            connection.disconnect();
        } catch (UploadException e) {
            throw e;
        } catch (Exception e) {
            throw new UploadException("POLICY_COMPARE_UPLOAD_STORAGE_ERROR", "文件写入对象存储失败。", e);
        }
    }

    private void sign(HttpURLConnection connection, URL base, String method, String canonicalUri,
                      String contentType, byte[] payload) throws Exception {
        ZonedDateTime now = ZonedDateTime.now(ZoneOffset.UTC);
        String amzDate = AMZ_DATE.format(now);
        String dateStamp = DATE_STAMP.format(now);
        String host = base.getPort() < 0 ? base.getHost() : base.getHost() + ":" + base.getPort();
        String payloadHash = sha256Hex(payload);
        String canonicalHeaders = "content-type:" + contentType + "\n"
                + "host:" + host + "\n"
                + "x-amz-content-sha256:" + payloadHash + "\n"
                + "x-amz-date:" + amzDate + "\n";
        String signedHeaders = "content-type;host;x-amz-content-sha256;x-amz-date";
        String canonicalRequest = method + "\n" + canonicalUri + "\n\n" + canonicalHeaders + "\n"
                + signedHeaders + "\n" + payloadHash;
        String scope = dateStamp + "/" + region + "/s3/aws4_request";
        String stringToSign = "AWS4-HMAC-SHA256\n" + amzDate + "\n" + scope + "\n"
                + sha256Hex(canonicalRequest.getBytes(StandardCharsets.UTF_8));
        byte[] signingKey = hmac(hmac(hmac(hmac(("AWS4" + secretKey).getBytes(StandardCharsets.UTF_8), dateStamp), region), "s3"), "aws4_request");
        String signature = hex(hmac(signingKey, stringToSign));

        connection.setRequestProperty("Content-Type", contentType);
        connection.setRequestProperty("Host", host);
        connection.setRequestProperty("x-amz-content-sha256", payloadHash);
        connection.setRequestProperty("x-amz-date", amzDate);
        connection.setRequestProperty("Authorization", "AWS4-HMAC-SHA256 Credential=" + accessKey + "/" + scope
                + ", SignedHeaders=" + signedHeaders + ", Signature=" + signature);
    }

    private byte[] hmac(byte[] key, String value) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(key, "HmacSHA256"));
        return mac.doFinal(value.getBytes(StandardCharsets.UTF_8));
    }

    private String sha256Hex(byte[] value) throws Exception {
        return hex(MessageDigest.getInstance("SHA-256").digest(value));
    }

    private String hex(byte[] value) {
        StringBuilder result = new StringBuilder(value.length * 2);
        for (byte b : value) result.append(String.format("%02x", b & 0xff));
        return result.toString();
    }

    private String encodePath(String value) {
        StringBuilder encoded = new StringBuilder();
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        for (byte b : bytes) {
            int c = b & 0xff;
            if ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9')
                    || c == '-' || c == '_' || c == '.' || c == '~' || c == '/') {
                encoded.append((char) c);
            } else {
                encoded.append('%').append(String.format("%02X", c));
            }
        }
        return encoded.toString();
    }

    private byte[] readAll(InputStream input, long limit) throws IOException, UploadException {
        try (InputStream in = input; ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[8192];
            int read;
            long total = 0;
            while ((read = in.read(buffer)) >= 0) {
                total += read;
                if (total > limit) throw new UploadException("POLICY_COMPARE_UPLOAD_TOO_LARGE", "文件大小不能超过 50MB。");
                output.write(buffer, 0, read);
            }
            return output.toByteArray();
        }
    }

    private String safeFilename(String original) throws UploadException {
        String filename = original == null ? "" : original.replace('\\', '/');
        filename = filename.substring(filename.lastIndexOf('/') + 1).trim();
        if (filename.isEmpty() || filename.contains("..")) {
            throw new UploadException("POLICY_COMPARE_UPLOAD_INVALID_NAME", "文件名不合法。");
        }
        return filename;
    }

    private String extension(String filename) {
        int dot = filename.lastIndexOf('.');
        return dot < 0 ? "" : filename.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    private String contentType(String extension) {
        if ("pdf".equals(extension)) return "application/pdf";
        return "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
    }

    private String readText(InputStream input) throws IOException {
        if (input == null) return "";
        byte[] bytes = readAllUnchecked(input, 2048);
        return new String(bytes, StandardCharsets.UTF_8).trim();
    }

    private byte[] readAllUnchecked(InputStream input, int limit) throws IOException {
        try (InputStream in = input; ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[512];
            int read;
            while ((read = in.read(buffer)) >= 0 && output.size() < limit) {
                output.write(buffer, 0, Math.min(read, limit - output.size()));
            }
            return output.toByteArray();
        }
    }

    private void validateConfiguration() throws UploadException {
        if (!enabled) throw new UploadException("POLICY_COMPARE_UPLOAD_DISABLED", "制度比对文件上传尚未启用。");
        if (blank(endpoint) || blank(bucket) || blank(accessKey) || blank(secretKey)) {
            throw new UploadException("POLICY_COMPARE_UPLOAD_NOT_CONFIGURED", "制度比对对象存储配置不完整。");
        }
    }

    private boolean blank(String value) {
        return value == null || value.trim().isEmpty();
    }

    public static class UploadException extends Exception {
        private final String code;

        UploadException(String code, String message) {
            super(message);
            this.code = code;
        }

        UploadException(String code, String message, Throwable cause) {
            super(message, cause);
            this.code = code;
        }

        public String getCode() {
            return code;
        }
    }
}
