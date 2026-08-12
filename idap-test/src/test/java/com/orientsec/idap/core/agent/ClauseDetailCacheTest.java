package com.orientsec.idap.core.agent;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ClauseDetailCacheTest {

    @Test
    void retainsRetrievedSourceTextAlongsideTheCitation() {
        Map<String, Object> citation = new HashMap<>();
        citation.put("clause_id", "C-1");
        citation.put("doc_title", "监管规则");

        Map<String, Object> detail = new HashMap<>();
        detail.put("clause_id", "C-1");
        detail.put("text", "第三条原文");

        Map<String, Object> answer = new HashMap<>();
        answer.put("basis", Arrays.<Object>asList(citation));
        answer.put("source_details", Arrays.<Object>asList(detail));
        Map<String, Object> upstream = new HashMap<>();
        upstream.put("answer", answer);

        ClauseDetailCache cache = new ClauseDetailCache();
        cache.remember(upstream);

        assertEquals("第三条原文", cache.find("C-1").get("text"));
    }
}
