package com.orientsec.idap.core.controller;

import com.orientsec.idap.core.agent.ClauseDetailCache;
import com.orientsec.idap.core.agent.RegulationQueryRequest;
import com.orientsec.idap.core.agent.TaskRuntimeQueryClient;
import com.orientsec.idap.core.agent.history.RegulationHistoryService;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Arrays;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

class RegulationQueryControllerTest {
    @Test
    @SuppressWarnings("unchecked")
    void rejectsNonQueryOperationsBeforeCallingTaskRuntime() {
        RegulationQueryController controller = new RegulationQueryController(
                mock(TaskRuntimeQueryClient.class),
                mock(ClauseDetailCache.class),
                mock(RegulationHistoryService.class));
        RegulationQueryRequest request = new RegulationQueryRequest();
        request.setQuestion("比较两个制度版本");
        request.setOperation("compare");

        ResponseEntity<Map<String, Object>> response = controller.query(request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        Map<String, Object> error = (Map<String, Object>) response.getBody().get("error");
        assertEquals("REGULATION_QUERY_OPERATION_UNSUPPORTED", error.get("code"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void rejectsAttachmentsAtTheQueryBoundary() {
        RegulationQueryController controller = new RegulationQueryController(
                mock(TaskRuntimeQueryClient.class),
                mock(ClauseDetailCache.class),
                mock(RegulationHistoryService.class));
        RegulationQueryRequest request = new RegulationQueryRequest();
        request.setQuestion("查询关联交易规则");
        request.setAttachmentFileIds(Arrays.asList("FILE-001"));

        ResponseEntity<Map<String, Object>> response = controller.query(request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        Map<String, Object> error = (Map<String, Object>) response.getBody().get("error");
        assertEquals("REGULATION_QUERY_ATTACHMENT_UNSUPPORTED", error.get("code"));
    }
}
