package com.orientsec.idap.core.agent.history;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class RegulationHistoryService {
    private static final int MAX_SESSION_ID_LENGTH = 128;
    private final Path root;
    private final Clock clock;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    public RegulationHistoryService(
            @Value("${REGULATION_HISTORY_DIR:${regulation.history.dir:./runtime/regulation-history}}") String root) {
        this(Paths.get(root), Clock.systemUTC());
    }

    public RegulationHistoryService(Path root, Clock clock) {
        this.root = root;
        this.clock = clock;
        ensureDirectory();
    }

    public synchronized void append(String sessionId, String queryId, String question,
                                    Map<String, Object> responseSnapshot) {
        validateSessionId(sessionId);
        RegulationHistorySession session = findInternal(sessionId);
        String now = Instant.now(clock).toString();
        if (session == null) {
            session = new RegulationHistorySession();
            session.setSessionId(sessionId);
            session.setTitle(abbreviate(question, 60));
            session.setCreatedAt(now);
        }

        RegulationHistoryTurn turn = new RegulationHistoryTurn();
        turn.setQueryId(queryId);
        turn.setQuestion(question);
        turn.setAnswer(extractAnswer(responseSnapshot));
        turn.setCreatedAt(now);
        turn.setResponseSnapshot(deepCopy(responseSnapshot));
        session.getTurns().add(turn);
        session.setSummary(abbreviate(turn.getAnswer(), 120));
        session.setUpdatedAt(now);
        write(session);
    }

    public synchronized RegulationHistorySession find(String sessionId) {
        validateSessionId(sessionId);
        RegulationHistorySession session = findInternal(sessionId);
        return session == null ? null : deepCopy(session, RegulationHistorySession.class);
    }

    public synchronized Page list(int page, int pageSize) {
        if (page < 1 || pageSize < 1 || pageSize > 100) {
            throw new IllegalArgumentException("page必须大于0，pageSize必须在1到100之间。");
        }
        List<RegulationHistorySession> sessions = readAll().stream()
                .sorted(Comparator.comparing(RegulationHistorySession::getUpdatedAt,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .collect(Collectors.toList());
        int from = Math.min((page - 1) * pageSize, sessions.size());
        int to = Math.min(from + pageSize, sessions.size());
        List<Summary> items = sessions.subList(from, to).stream().map(Summary::new).collect(Collectors.toList());
        return new Page(items, page, pageSize, sessions.size());
    }

    private List<RegulationHistorySession> readAll() {
        ensureDirectory();
        List<RegulationHistorySession> sessions = new ArrayList<>();
        try (java.util.stream.Stream<Path> paths = Files.list(root)) {
            paths.filter(path -> path.getFileName().toString().endsWith(".json")).forEach(path -> {
                try {
                    sessions.add(objectMapper.readValue(path.toFile(), RegulationHistorySession.class));
                } catch (IOException ignored) {
                    // A damaged history file must not make all other sessions unavailable.
                }
            });
        } catch (IOException e) {
            throw new IllegalStateException("无法读取制度查询历史。", e);
        }
        return sessions;
    }

    private RegulationHistorySession findInternal(String sessionId) {
        Path path = sessionPath(sessionId);
        if (!Files.exists(path)) return null;
        try {
            return objectMapper.readValue(path.toFile(), RegulationHistorySession.class);
        } catch (IOException e) {
            throw new IllegalStateException("制度查询历史读取失败。", e);
        }
    }

    private void write(RegulationHistorySession session) {
        ensureDirectory();
        Path temp = null;
        try {
            temp = Files.createTempFile(root, "regulation-history-", ".tmp");
            objectMapper.writeValue(temp.toFile(), session);
            try {
                Files.move(temp, sessionPath(session.getSessionId()), StandardCopyOption.ATOMIC_MOVE,
                        StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(temp, sessionPath(session.getSessionId()), StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            throw new IllegalStateException("制度查询历史保存失败。", e);
        } finally {
            if (temp != null) try { Files.deleteIfExists(temp); } catch (IOException ignored) { }
        }
    }

    private void ensureDirectory() {
        try {
            Files.createDirectories(root);
        } catch (IOException e) {
            throw new IllegalStateException("无法创建制度查询历史目录。", e);
        }
    }

    private Path sessionPath(String sessionId) { return root.resolve(sessionId + ".json"); }

    private void validateSessionId(String sessionId) {
        if (sessionId == null || sessionId.length() > MAX_SESSION_ID_LENGTH ||
                !sessionId.matches("[A-Za-z0-9._-]+")) {
            throw new IllegalArgumentException("无效的会话标识。");
        }
    }

    @SuppressWarnings("unchecked")
    private String extractAnswer(Map<String, Object> response) {
        Object blocks = response == null ? null : response.get("answer_blocks");
        if (!(blocks instanceof List)) return "";
        StringBuilder answer = new StringBuilder();
        for (Object item : (List<?>) blocks) {
            if (!(item instanceof Map)) continue;
            Object content = ((Map<String, Object>) item).get("content");
            if (content instanceof String) answer.append(content);
        }
        return answer.toString();
    }

    private String abbreviate(String value, int max) {
        if (value == null) return "";
        String text = value.trim();
        return text.length() <= max ? text : text.substring(0, max) + "…";
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> deepCopy(Map<String, Object> value) {
        return objectMapper.convertValue(value == null ? new LinkedHashMap<>() : value, LinkedHashMap.class);
    }

    private <T> T deepCopy(Object value, Class<T> type) { return objectMapper.convertValue(value, type); }

    public static class Summary {
        private final String sessionId;
        private final String title;
        private final String summary;
        private final String createdAt;
        private final String updatedAt;
        private final int turnCount;

        Summary(RegulationHistorySession session) {
            this.sessionId = session.getSessionId();
            this.title = session.getTitle();
            this.summary = session.getSummary();
            this.createdAt = session.getCreatedAt();
            this.updatedAt = session.getUpdatedAt();
            this.turnCount = session.getTurns().size();
        }
        public String getSessionId() { return sessionId; }
        public String getTitle() { return title; }
        public String getSummary() { return summary; }
        public String getCreatedAt() { return createdAt; }
        public String getUpdatedAt() { return updatedAt; }
        public int getTurnCount() { return turnCount; }
    }

    public static class Page {
        private final List<Summary> items;
        private final int page;
        private final int pageSize;
        private final int total;
        Page(List<Summary> items, int page, int pageSize, int total) {
            this.items = items; this.page = page; this.pageSize = pageSize; this.total = total;
        }
        public List<Summary> getItems() { return items; }
        public int getPage() { return page; }
        public int getPageSize() { return pageSize; }
        public int getTotal() { return total; }
    }
}
