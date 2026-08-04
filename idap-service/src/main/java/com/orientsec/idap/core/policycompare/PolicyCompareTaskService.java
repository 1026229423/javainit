package com.orientsec.idap.core.policycompare;

import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Transitional task registry. It deliberately stores only task metadata until
 * task-runtime provides a policy-compare executor; no comparison result is invented here.
 */
@Service
public class PolicyCompareTaskService {
    public static final String EXTERNAL_VERSION = "externalVersion";
    public static final String INTERNAL_COVERAGE = "internalCoverage";
    public static final String PENDING_EXECUTOR = "PENDING_EXECUTOR";
    private static final int MAX_OBJECT_LENGTH = 500;
    private static final int MAX_SCOPE_ITEMS = 50;
    private static final int MAX_SCOPE_ITEM_LENGTH = 200;
    private final Map<String, PolicyCompareTaskView> tasks = new ConcurrentHashMap<>();

    public PolicyCompareTaskView create(PolicyCompareTaskCreateRequest request) throws ValidationException {
        validateCreate(request);
        String now = Instant.now().toString();
        PolicyCompareTaskView task = new PolicyCompareTaskView();
        task.setTaskId("PC-" + UUID.randomUUID().toString());
        task.setMode(request.getMode().trim());
        task.setStatus(PENDING_EXECUTOR);
        task.setPrimaryObject(request.getPrimaryObject().trim());
        task.setSecondaryObject(request.getSecondaryObject().trim());
        task.setScope(copyScope(request.getScope()));
        task.setCreatedAt(now);
        task.setUpdatedAt(now);
        task.setMessage("比对任务已创建，等待执行器接入。");
        task.setResult(null);
        tasks.put(task.getTaskId(), task);
        return copy(task);
    }

    public PolicyCompareTaskView find(String taskId) {
        PolicyCompareTaskView task = tasks.get(taskId);
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
        validateText(request.getPrimaryObject(), "比对对象 A");
        validateText(request.getSecondaryObject(), "比对对象 B");
        PolicyCompareTaskCreateRequest.Scope scope = request.getScope();
        if (scope != null) {
            validateItems(scope.getOrganizations(), "适用组织");
            validateItems(scope.getBusinessScenes(), "业务场景");
            validateItems(scope.getChapters(), "章节范围");
            validateDateRange(scope.getEffectiveDateRange());
        }
        List<String> attachmentFileIds = request.getAttachmentFileIds();
        if (attachmentFileIds != null && attachmentFileIds.size() > 1) {
            throw new ValidationException("当前一次比对仅支持一个附件标识。");
        }
        validateItems(attachmentFileIds, "附件标识");
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
        copy.setStatus(source.getStatus());
        copy.setPrimaryObject(source.getPrimaryObject());
        copy.setSecondaryObject(source.getSecondaryObject());
        copy.setScope(copyScope(source.getScope()));
        copy.setCreatedAt(source.getCreatedAt());
        copy.setUpdatedAt(source.getUpdatedAt());
        copy.setMessage(source.getMessage());
        copy.setResult(source.getResult() == null ? null : new HashMap<>(source.getResult()));
        return copy;
    }

    private PolicyCompareTaskCreateRequest.Scope copyScope(PolicyCompareTaskCreateRequest.Scope source) {
        PolicyCompareTaskCreateRequest.Scope copy = new PolicyCompareTaskCreateRequest.Scope();
        if (source == null) return copy;
        copy.setOrganizations(copyList(source.getOrganizations()));
        copy.setBusinessScenes(copyList(source.getBusinessScenes()));
        copy.setChapters(copyList(source.getChapters()));
        copy.setEffectiveDateRange(copyList(source.getEffectiveDateRange()));
        return copy;
    }

    private List<String> copyList(List<String> source) {
        return source == null ? new ArrayList<String>() : new ArrayList<>(source);
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    public static class ValidationException extends Exception {
        ValidationException(String message) { super(message); }
    }
}
