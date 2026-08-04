package com.orientsec.idap.core.policycompare;

import com.orientsec.idap.core.controller.PolicyCompareTaskController;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Arrays;
import java.util.Map;

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
        coverage.setPrimaryObject("证券经营机构费用报销合规要求");
        coverage.setSecondaryObject("费用报销管理办法 · 2026版");

        ResponseEntity<?> creation = controller.create(coverage);
        assertEquals(HttpStatus.CREATED, creation.getStatusCode());
        assertEquals("internalCoverage", ((PolicyCompareTaskView) creation.getBody()).getMode());

        ResponseEntity<?> page = controller.list("internalCoverage", 1, 10);
        assertEquals(HttpStatus.OK, page.getStatusCode());
        PolicyCompareTaskPageView body = (PolicyCompareTaskPageView) page.getBody();
        assertEquals(1L, body.getTotal());
        assertEquals(1, body.getItems().size());
        assertEquals("internalCoverage", body.getItems().get(0).getMode());
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

    private PolicyCompareTaskCreateRequest externalVersionRequest() {
        PolicyCompareTaskCreateRequest request = new PolicyCompareTaskCreateRequest();
        request.setMode("externalVersion");
        request.setPrimaryObject("上市公司信息披露管理办法 · 2026版");
        request.setSecondaryObject("上市公司信息披露管理办法 · 2024版");
        PolicyCompareTaskCreateRequest.Scope scope = new PolicyCompareTaskCreateRequest.Scope();
        scope.setOrganizations(Arrays.asList("东方证券股份有限公司"));
        scope.setBusinessScenes(Arrays.asList("信息披露"));
        scope.setChapters(Arrays.asList("第八条"));
        scope.setEffectiveDateRange(Arrays.asList("2024-01-01", "2026-12-31"));
        request.setScope(scope);
        return request;
    }
}
