package com.orientsec.idap.core.agent;

import com.orientsec.idap.core.agent.history.RegulationHistoryService;
import com.orientsec.idap.core.agent.history.RegulationHistorySession;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class RegulationHistoryServiceTest {
    @TempDir
    Path tempDir;

    @Test
    void appendsTurnsAndReloadsCompleteResponseSnapshots() {
        RegulationHistoryService service = serviceAt("2026-08-07T10:00:00Z");

        service.append("session-1", "query-1", "什么是内幕信息知情人报送", response("第一轮回答", 3));
        service.append("session-1", "query-2", "有哪些相关处罚案例", response("第二轮回答", 5));

        RegulationHistorySession saved = service.find("session-1");
        assertNotNull(saved);
        assertEquals(2, saved.getTurns().size());
        assertEquals("什么是内幕信息知情人报送", saved.getTitle());
        assertEquals("第二轮回答", saved.getSummary());
        assertEquals(5, ((Number) saved.getTurns().get(1).getResponseSnapshot()
                .get("result_count")).intValue());

        RegulationHistoryService reloaded = serviceAt("2026-08-07T10:01:00Z");
        RegulationHistorySession restored = reloaded.find("session-1");
        assertNotNull(restored);
        assertEquals(2, restored.getTurns().size());
        assertEquals("第二轮回答", restored.getTurns().get(1).getAnswer());
    }

    @Test
    void listsLatestSessionsFirstWithPagination() {
        RegulationHistoryService first = serviceAt("2026-08-07T10:00:00Z");
        first.append("session-old", "query-1", "旧问题", response("旧回答", 1));

        RegulationHistoryService second = serviceAt("2026-08-07T11:00:00Z");
        second.append("session-new", "query-2", "新问题", response("新回答", 2));

        RegulationHistoryService.Page page = second.list(1, 1);
        assertEquals(2, page.getTotal());
        assertEquals(1, page.getItems().size());
        assertEquals("session-new", page.getItems().get(0).getSessionId());
    }

    private RegulationHistoryService serviceAt(String instant) {
        return new RegulationHistoryService(tempDir, Clock.fixed(Instant.parse(instant), ZoneOffset.UTC));
    }

    private Map<String, Object> response(String answer, int resultCount) {
        Map<String, Object> answerBlock = new LinkedHashMap<>();
        answerBlock.put("type", "text");
        answerBlock.put("content", answer);
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("answer_blocks", Collections.singletonList(answerBlock));
        response.put("result_count", resultCount);
        return response;
    }
}
