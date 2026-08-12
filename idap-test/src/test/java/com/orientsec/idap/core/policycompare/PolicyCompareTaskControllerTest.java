package com.orientsec.idap.core.policycompare;

import com.orientsec.idap.core.controller.PolicyCompareTaskController;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class PolicyCompareTaskControllerTest {

    @Test
    public void createsExternalVersionTaskWithoutInventingComparisonResult() {
        PolicyCompareTaskController controller = new PolicyCompareTaskController(new PolicyCompareTaskService());

        ResponseEntity<?> response = controller.create(externalVersionRequest());

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        PolicyCompareTaskView task = (PolicyCompareTaskView) response.getBody();
        assertNotNull(task);
        assertTrue(task.getTaskId().startsWith("PC-"));
        assertEquals("externalVersion", task.getMode());
        assertEquals("PENDING_EXECUTOR", task.getStatus());
        assertEquals("上市公司信息披露管理办法 · 2026版", task.getPrimaryObject());
        assertEquals("上市公司信息披露管理办法 · 2024版", task.getSecondaryObject());
        assertNull(task.getResult());

        ResponseEntity<?> detail = controller.detail(task.getTaskId());
        assertEquals(HttpStatus.OK, detail.getStatusCode());
        assertEquals(task.getTaskId(), ((PolicyCompareTaskView) detail.getBody()).getTaskId());
    }

    @Test
    public void createsInternalCoverageTaskAndSupportsPaginatedHistory() {
        PolicyCompareTaskController controller = new PolicyCompareTaskController(new PolicyCompareTaskService());
        controller.create(externalVersionRequest());
        PolicyCompareTaskCreateRequest coverage = new PolicyCompareTaskCreateRequest();
        coverage.setMode("internalCoverage");
        coverage.setPrimaryObjectSource("library");
        coverage.setPrimaryObject("证券经营机构费用报销合规要求");
        coverage.setPrimaryDocVersionId("DOC-VERSION-001");
        coverage.setSecondaryObject("客户端错误指定的外规库 · 2026版");

        ResponseEntity<?> creation = controller.create(coverage);
        assertEquals(HttpStatus.CREATED, creation.getStatusCode());
        assertEquals("internalCoverage", ((PolicyCompareTaskView) creation.getBody()).getMode());
        assertEquals("内部制度库", ((PolicyCompareTaskView) creation.getBody()).getSecondaryObject());

        ResponseEntity<?> page = controller.list("internalCoverage", 1, 10);
        assertEquals(HttpStatus.OK, page.getStatusCode());
        PolicyCompareTaskPageView body = (PolicyCompareTaskPageView) page.getBody();
        assertEquals(1L, body.getTotal());
        assertEquals(1, body.getItems().size());
        assertEquals("internalCoverage", body.getItems().get(0).getMode());
    }

    @Test
    public void defaultsUploadedCoverageTargetToTheAuthorizedInternalPolicyLibrary() {
        PolicyCompareTaskController controller = new PolicyCompareTaskController(new PolicyCompareTaskService());
        PolicyCompareTaskCreateRequest coverage = new PolicyCompareTaskCreateRequest();
        coverage.setMode("internalCoverage");
        coverage.setPrimaryObjectSource("upload");
        coverage.setSecondaryObject("");
        coverage.setAttachmentFileIds(Arrays.asList("FILE-001"));
        PolicyCompareTaskCreateRequest.ExternalArtifact artifact = new PolicyCompareTaskCreateRequest.ExternalArtifact();
        artifact.setObjectKey("upload/FILE-001/policy.docx");
        artifact.setUploadId("FILE-001");
        artifact.setFilename("policy.docx");
        coverage.setExternalArtifact(artifact);

        ResponseEntity<?> response = controller.create(coverage);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        PolicyCompareTaskView task = (PolicyCompareTaskView) response.getBody();
        assertNotNull(task);
        assertEquals("policy.docx", task.getPrimaryObject());
        assertEquals("内部制度库", task.getSecondaryObject());
    }

    @Test
    public void internalCoverageSubjectAutomaticallyTargetsExternalRuleLibrary() {
        PolicyCompareTaskController controller = new PolicyCompareTaskController(new PolicyCompareTaskService());
        PolicyCompareTaskCreateRequest coverage = new PolicyCompareTaskCreateRequest();
        coverage.setMode("internalCoverage");
        coverage.setCoverageSubjectType("internal");
        coverage.setPrimaryObjectSource("library");
        coverage.setPrimaryObject("费用报销管理办法 · 2026版");
        coverage.setPrimaryDocVersionId("INT-DV-1");
        coverage.setSecondaryObject("客户端错误指定的内部制度库");

        ResponseEntity<?> response = controller.create(coverage);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        PolicyCompareTaskView task = (PolicyCompareTaskView) response.getBody();
        assertNotNull(task);
        assertEquals("internal", task.getCoverageSubjectType());
        assertEquals("外部规则库", task.getSecondaryObject());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void rejectsUnknownModeAtTheHttpBoundary() {
        PolicyCompareTaskController controller = new PolicyCompareTaskController(new PolicyCompareTaskService());
        PolicyCompareTaskCreateRequest request = externalVersionRequest();
        request.setMode("external");

        ResponseEntity<?> response = controller.create(request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        Map<String, Object> error = (Map<String, Object>) body.get("error");
        assertEquals("POLICY_COMPARE_INVALID_REQUEST", error.get("code"));
    }

    @Test
    public void retainsTheSingleComparisonAttachmentId() {
        PolicyCompareTaskController controller = new PolicyCompareTaskController(new PolicyCompareTaskService());
        PolicyCompareTaskCreateRequest request = externalVersionRequest();
        request.setPrimaryObject(null);
        request.setPrimaryObjectSource("upload");
        request.setAttachmentFileIds(Arrays.asList("FILE-001"));

        ResponseEntity<?> response = controller.create(request);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        PolicyCompareTaskView task = (PolicyCompareTaskView) response.getBody();
        assertNotNull(task);
        assertEquals(Arrays.asList("FILE-001"), task.getAttachmentFileIds());
        assertEquals("upload", task.getPrimaryObjectSource());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void rejectsAnAttachmentWhenThePrimaryPolicyComesFromTheLibrary() {
        PolicyCompareTaskController controller = new PolicyCompareTaskController(new PolicyCompareTaskService());
        PolicyCompareTaskCreateRequest request = externalVersionRequest();
        request.setPrimaryObjectSource("library");
        request.setAttachmentFileIds(Arrays.asList("FILE-001"));

        ResponseEntity<?> response = controller.create(request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        Map<String, Object> error = (Map<String, Object>) body.get("error");
        assertEquals("制度库选择与文件上传不能同时使用。", error.get("message"));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void rejectsMoreThanOneComparisonAttachment() {
        PolicyCompareTaskController controller = new PolicyCompareTaskController(new PolicyCompareTaskService());
        PolicyCompareTaskCreateRequest request = externalVersionRequest();
        request.setAttachmentFileIds(Arrays.asList("FILE-001", "FILE-002"));

        ResponseEntity<?> response = controller.create(request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        Map<String, Object> error = (Map<String, Object>) body.get("error");
        assertEquals("POLICY_COMPARE_INVALID_REQUEST", error.get("code"));
        assertEquals("当前一次比对仅支持一个附件标识。", error.get("message"));
    }

    @Test
    public void scopeContractOnlyExposesEffectiveDateRange() {
        Set<String> fieldNames = Arrays.stream(PolicyCompareTaskCreateRequest.Scope.class.getDeclaredFields())
                .map(field -> field.getName())
                .collect(Collectors.toCollection(HashSet::new));

        assertEquals(new HashSet<>(Arrays.asList("effectiveDateRange")), fieldNames);
    }

    private PolicyCompareTaskCreateRequest externalVersionRequest() {
        PolicyCompareTaskCreateRequest request = new PolicyCompareTaskCreateRequest();
        request.setMode("externalVersion");
        request.setPrimaryObject("上市公司信息披露管理办法 · 2026版");
        request.setSecondaryObject("上市公司信息披露管理办法 · 2024版");
        PolicyCompareTaskCreateRequest.Scope scope = new PolicyCompareTaskCreateRequest.Scope();
        scope.setEffectiveDateRange(Arrays.asList("2024-01-01", "2026-12-31"));
        request.setScope(scope);
        return request;
    }
}
