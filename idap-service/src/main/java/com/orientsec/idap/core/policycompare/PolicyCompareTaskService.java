package com.orientsec.idap.core.policycompare;

import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Local task registry backed by task-runtime for the policy-compare capabilities that
 * task-runtime currently publishes. Unsupported modes remain explicit pending tasks.
 */
@Service
public class PolicyCompareTaskService {
    public static final String EXTERNAL_VERSION = "externalVersion";
    public static final String INTERNAL_COVERAGE = "internalCoverage";
    public static final String PRIMARY_SOURCE_LIBRARY = "library";
    public static final String PRIMARY_SOURCE_UPLOAD = "upload";
    public static final String COVERAGE_SUBJECT_EXTERNAL = "external";
    public static final String COVERAGE_SUBJECT_INTERNAL = "internal";
    public static final String PENDING_EXECUTOR = "PENDING_EXECUTOR";
    private static final int MAX_OBJECT_LENGTH = 500;
    private static final int MAX_SCOPE_ITEMS = 50;
    private static final int MAX_SCOPE_ITEM_LENGTH = 200;
    private final Map<String, PolicyCompareTaskView> tasks = new ConcurrentHashMap<>();
    private final TaskRuntimePolicyCompareClient taskRuntimeClient;

    /** Kept for isolated legacy controller tests; production uses the injected constructor. */
    public PolicyCompareTaskService() {
        this.taskRuntimeClient = null;
    }

    @Autowired
    public PolicyCompareTaskService(TaskRuntimePolicyCompareClient taskRuntimeClient) {
        this.taskRuntimeClient = taskRuntimeClient;
    }

    public PolicyCompareTaskView create(PolicyCompareTaskCreateRequest request) throws ValidationException, ExecutionException {
        validateCreate(request);
        String now = Instant.now().toString();
        PolicyCompareTaskView task = new PolicyCompareTaskView();
        task.setTaskId("PC-" + UUID.randomUUID().toString());
        task.setMode(request.getMode().trim());
        task.setCoverageSubjectType(normalizeCoverageSubjectType(request.getCoverageSubjectType()));
        task.setStatus(PENDING_EXECUTOR);
        task.setPrimaryObjectSource(normalizePrimaryObjectSource(request.getPrimaryObjectSource()));
        task.setPrimaryObject(normalizedPrimaryObject(request));
        task.setPrimaryDocVersionId(request.getPrimaryDocVersionId());
        task.setSecondaryObject(normalizedSecondaryObject(request));
        task.setSecondaryDocVersionId(request.getSecondaryDocVersionId());
        task.setAttachmentFileIds(copyList(request.getAttachmentFileIds()));
        task.setScope(copyScope(request.getScope()));
        task.setCreatedAt(now);
        task.setUpdatedAt(now);
        task.setMessage("比对任务已创建。");
        task.setResult(null);
        tasks.put(task.getTaskId(), task);

        if (INTERNAL_COVERAGE.equals(task.getMode())) {
            if (taskRuntimeClient == null) {
                task.setStatus(PENDING_EXECUTOR);
                task.setMessage("覆盖比对执行器未注入。当前任务仅保留元数据。");
            } else {
                try {
                    applyRuntimeResponse(task, taskRuntimeClient.submitCoverage(request, task.getTaskId()));
                } catch (TaskRuntimePolicyCompareClient.CompareException e) {
                    tasks.remove(task.getTaskId());
                    throw new ExecutionException(e.getCode(), e.getMessage());
                }
            }
        } else if (EXTERNAL_VERSION.equals(task.getMode())) {
            if (taskRuntimeClient == null) {
                task.setStatus(PENDING_EXECUTOR);
                task.setMessage("版本差异比对执行器未注入。当前任务仅保留元数据。");
            } else {
                try {
                    applyRuntimeResponse(task, taskRuntimeClient.submitVersionDiff(request, task.getTaskId()));
                } catch (TaskRuntimePolicyCompareClient.CompareException e) {
                    tasks.remove(task.getTaskId());
                    throw new ExecutionException(e.getCode(), e.getMessage());
                }
            }
        } else {
            task.setStatus(PENDING_EXECUTOR);
            task.setMessage(EXTERNAL_VERSION.equals(task.getMode())
                    ? "Pi 当前仅发布覆盖比对任务类型，差异比对尚未接入执行器。"
                    : "覆盖比对执行器尚未接入。");
        }
        return copy(task);
    }

    public PolicyCompareTaskView find(String taskId) throws ExecutionException {
        PolicyCompareTaskView task = tasks.get(taskId);
        if (task != null && taskRuntimeClient != null && !isBlank(task.getRuntimeRunId()) && isRunning(task.getStatus())) {
            try {
                applyRuntimeResponse(task, taskRuntimeClient.findRun(task.getRuntimeRunId()));
            } catch (TaskRuntimePolicyCompareClient.CompareException e) {
                throw new ExecutionException(e.getCode(), e.getMessage());
            }
        }
        return task == null ? null : copy(task);
    }

    public PolicyCompareTaskPageView list(String mode, int page, int pageSize) throws ValidationException {
        if (page < 1 || pageSize < 1 || pageSize > 100) {
            throw new ValidationException("分页参数不合法。");
        }
        if (!isBlank(mode) && !isSupportedMode(mode)) {
            throw new ValidationException("核查模式仅支持 externalVersion 或 internalCoverage。");
        }
        List<PolicyCompareTaskView> matched = new ArrayList<>();
        for (PolicyCompareTaskView task : tasks.values()) {
            if (isBlank(mode) || mode.equals(task.getMode())) matched.add(copy(task));
        }
        matched.sort(Comparator.comparing(PolicyCompareTaskView::getCreatedAt).reversed());
        int from = Math.min((page - 1) * pageSize, matched.size());
        int to = Math.min(from + pageSize, matched.size());
        PolicyCompareTaskPageView response = new PolicyCompareTaskPageView();
        response.setItems(new ArrayList<>(matched.subList(from, to)));
        response.setPage(page);
        response.setPageSize(pageSize);
        response.setTotal(matched.size());
        return response;
    }

    private void validateCreate(PolicyCompareTaskCreateRequest request) throws ValidationException {
        if (request == null) throw new ValidationException("请求体不能为空。");
        if (!isSupportedMode(request.getMode())) {
            throw new ValidationException("核查模式仅支持 externalVersion 或 internalCoverage。");
        }
        String primaryObjectSource = normalizePrimaryObjectSource(request.getPrimaryObjectSource());
        String coverageSubjectType = normalizeCoverageSubjectType(request.getCoverageSubjectType());
        if (EXTERNAL_VERSION.equals(request.getMode())) validateVersionDiff(request, primaryObjectSource);
        PolicyCompareTaskCreateRequest.Scope scope = request.getScope();
        if (scope != null) {
            validateDateRange(scope.getEffectiveDateRange());
        }
        List<String> attachmentFileIds = request.getAttachmentFileIds();
        if (attachmentFileIds != null && attachmentFileIds.size() > 1) {
            throw new ValidationException("当前一次比对仅支持一个附件标识。");
        }
        validateItems(attachmentFileIds, "附件标识");
        if (PRIMARY_SOURCE_LIBRARY.equals(primaryObjectSource)) {
            validateText(request.getPrimaryObject(), "比对对象 A");
            if (INTERNAL_COVERAGE.equals(request.getMode())) {
                validateText(request.getPrimaryDocVersionId(), COVERAGE_SUBJECT_INTERNAL.equals(coverageSubjectType)
                        ? "知识库内规版本标识" : "知识库外规版本标识");
            }
            if (request.getExternalArtifact() != null) {
                throw new ValidationException("制度库选择与上传文件引用不能同时使用。");
            }
            if (attachmentFileIds != null && !attachmentFileIds.isEmpty()) {
                throw new ValidationException("制度库选择与文件上传不能同时使用。");
            }
        } else if (attachmentFileIds == null || attachmentFileIds.size() != 1) {
            if (request.getExternalArtifact() == null) {
                throw new ValidationException("上传文件时必须提供且仅提供一个附件标识。");
            }
        }
        if (INTERNAL_COVERAGE.equals(request.getMode()) && PRIMARY_SOURCE_UPLOAD.equals(primaryObjectSource)) {
            validateExternalArtifact(request.getExternalArtifact());
        }
    }

    private void validateVersionDiff(PolicyCompareTaskCreateRequest request, String primaryObjectSource)
            throws ValidationException {
        if (!PRIMARY_SOURCE_LIBRARY.equals(primaryObjectSource)) {
            throw new ValidationException("版本差异比对仅支持从知识库选择同一制度的两个版本。");
        }
        validateText(request.getPrimaryObject(), "比对对象 A");
        validateText(request.getSecondaryObject(), "比对对象 B");
        validateText(request.getPrimaryDocVersionId(), "新版本标识");
        validateText(request.getSecondaryDocVersionId(), "旧版本标识");
        if (request.getPrimaryDocVersionId().trim().equals(request.getSecondaryDocVersionId().trim())) {
            throw new ValidationException("新旧版本必须不同。");
        }
    }

    private String normalizedSecondaryObject(PolicyCompareTaskCreateRequest request) throws ValidationException {
        if (INTERNAL_COVERAGE.equals(request.getMode())) {
            return COVERAGE_SUBJECT_INTERNAL.equals(normalizeCoverageSubjectType(request.getCoverageSubjectType()))
                    ? "外部规则库" : "内部制度库";
        }
        return request.getSecondaryObject().trim();
    }

    private String normalizedPrimaryObject(PolicyCompareTaskCreateRequest request) throws ValidationException {
        if (!isBlank(request.getPrimaryObject())) return request.getPrimaryObject().trim();
        if (PRIMARY_SOURCE_UPLOAD.equals(normalizePrimaryObjectSource(request.getPrimaryObjectSource()))
                && request.getExternalArtifact() != null
                && !isBlank(request.getExternalArtifact().getFilename())) {
            return request.getExternalArtifact().getFilename().trim();
        }
        return "上传文件（待解析）";
    }

    private void validateExternalArtifact(PolicyCompareTaskCreateRequest.ExternalArtifact artifact)
            throws ValidationException {
        if (artifact == null || isBlank(artifact.getObjectKey()) || isBlank(artifact.getUploadId())
                || isBlank(artifact.getFilename())) {
            throw new ValidationException("覆盖比对上传文件缺少 objectKey、uploadId 或 filename。");
        }
    }

    @SuppressWarnings("unchecked")
    private void applyRuntimeResponse(PolicyCompareTaskView task, Map<String, Object> response) {
        String status = response.get("status") == null ? "" : String.valueOf(response.get("status"));
        task.setRuntimeRunId(response.get("runId") == null ? task.getRuntimeRunId() : String.valueOf(response.get("runId")));
        task.setStatus(status.toUpperCase(Locale.ROOT));
        task.setUpdatedAt(Instant.now().toString());
        task.setProgress(response.get("progress") instanceof Map
                ? new HashMap<>((Map<String, Object>) response.get("progress")) : null);
        if ("completed".equals(status) && response.get("answer") instanceof Map) {
            task.setResult(new HashMap<>((Map<String, Object>) response.get("answer")));
            task.setMessage(INTERNAL_COVERAGE.equals(task.getMode()) ? "制度覆盖比对已完成。" : "制度版本差异比对已完成。");
        } else if ("queued".equals(status) || "running".equals(status)) {
            task.setMessage(progressMessage(task.getProgress(), INTERNAL_COVERAGE.equals(task.getMode())
                    ? "制度覆盖比对正在执行。" : "制度版本差异比对正在执行。"));
        } else if ("error".equals(status) || "aborted".equals(status) || "limit_exceeded".equals(status)) {
            task.setResult(null);
            task.setMessage(response.get("errorMessage") == null
                    ? (INTERNAL_COVERAGE.equals(task.getMode()) ? "制度覆盖比对未能完成。" : "制度版本差异比对未能完成。")
                    : String.valueOf(response.get("errorMessage")));
        }
        tasks.put(task.getTaskId(), task);
    }

    private String progressMessage(Map<String, Object> progress, String fallback) {
        if (progress == null || progress.get("message") == null) return fallback;
        String message = String.valueOf(progress.get("message"));
        return isBlank(message) ? fallback : message;
    }

    private boolean isRunning(String status) {
        return "QUEUED".equals(status) || "RUNNING".equals(status);
    }

    private void validateText(String value, String name) throws ValidationException {
        if (isBlank(value)) throw new ValidationException(name + "不能为空。");
        if (value.trim().length() > MAX_OBJECT_LENGTH) throw new ValidationException(name + "不能超过500个字符。");
    }

    private void validateItems(List<String> values, String name) throws ValidationException {
        if (values == null) return;
        if (values.size() > MAX_SCOPE_ITEMS) throw new ValidationException(name + "不能超过50项。");
        for (String value : values) {
            if (isBlank(value) || value.trim().length() > MAX_SCOPE_ITEM_LENGTH) {
                throw new ValidationException(name + "包含不合法项。");
            }
        }
    }

    private void validateDateRange(List<String> range) throws ValidationException {
        if (range == null || range.isEmpty()) return;
        if (range.size() != 2) throw new ValidationException("生效日期区间必须同时提供开始和结束日期。");
        try {
            LocalDate start = LocalDate.parse(range.get(0));
            LocalDate end = LocalDate.parse(range.get(1));
            if (start.isAfter(end)) throw new ValidationException("生效日期区间的开始日期不能晚于结束日期。");
        } catch (DateTimeParseException e) {
            throw new ValidationException("生效日期必须采用 YYYY-MM-DD 格式。");
        }
    }

    private boolean isSupportedMode(String mode) {
        return EXTERNAL_VERSION.equals(mode) || INTERNAL_COVERAGE.equals(mode);
    }

    private PolicyCompareTaskView copy(PolicyCompareTaskView source) {
        PolicyCompareTaskView copy = new PolicyCompareTaskView();
        copy.setTaskId(source.getTaskId());
        copy.setMode(source.getMode());
        copy.setCoverageSubjectType(source.getCoverageSubjectType());
        copy.setStatus(source.getStatus());
        copy.setPrimaryObjectSource(source.getPrimaryObjectSource());
        copy.setPrimaryObject(source.getPrimaryObject());
        copy.setPrimaryDocVersionId(source.getPrimaryDocVersionId());
        copy.setSecondaryObject(source.getSecondaryObject());
        copy.setSecondaryDocVersionId(source.getSecondaryDocVersionId());
        copy.setAttachmentFileIds(copyList(source.getAttachmentFileIds()));
        copy.setScope(copyScope(source.getScope()));
        copy.setCreatedAt(source.getCreatedAt());
        copy.setUpdatedAt(source.getUpdatedAt());
        copy.setMessage(source.getMessage());
        copy.setResult(source.getResult() == null ? null : new HashMap<>(source.getResult()));
        copy.setRuntimeRunId(source.getRuntimeRunId());
        copy.setProgress(source.getProgress() == null ? null : new HashMap<>(source.getProgress()));
        return copy;
    }

    private PolicyCompareTaskCreateRequest.Scope copyScope(PolicyCompareTaskCreateRequest.Scope source) {
        PolicyCompareTaskCreateRequest.Scope copy = new PolicyCompareTaskCreateRequest.Scope();
        if (source == null) return copy;
        copy.setEffectiveDateRange(copyList(source.getEffectiveDateRange()));
        return copy;
    }

    private List<String> copyList(List<String> source) {
        return source == null ? new ArrayList<String>() : new ArrayList<>(source);
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private String normalizePrimaryObjectSource(String source) throws ValidationException {
        if (isBlank(source)) return PRIMARY_SOURCE_LIBRARY;
        String normalized = source.trim();
        if (PRIMARY_SOURCE_LIBRARY.equals(normalized) || PRIMARY_SOURCE_UPLOAD.equals(normalized)) {
            return normalized;
        }
        throw new ValidationException("比对对象 A 来源仅支持 library 或 upload。");
    }

    private String normalizeCoverageSubjectType(String subjectType) throws ValidationException {
        if (isBlank(subjectType)) return COVERAGE_SUBJECT_EXTERNAL;
        String normalized = subjectType.trim();
        if (COVERAGE_SUBJECT_EXTERNAL.equals(normalized) || COVERAGE_SUBJECT_INTERNAL.equals(normalized)) {
            return normalized;
        }
        throw new ValidationException("覆盖比对待核查制度类型仅支持 external 或 internal。");
    }

    public static class ValidationException extends Exception {
        ValidationException(String message) { super(message); }
    }

    public static class ExecutionException extends Exception {
        private final String code;

        ExecutionException(String code, String message) {
            super(message);
            this.code = code;
        }

        public String getCode() {
            return code;
        }
    }
}
