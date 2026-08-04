# 制度比对任务 API

## 状态

已实现 Java 的任务资源边界；真实比对执行器尚未接入 task-runtime。

Java 不直接调用 audit-ai，也不会为未执行的任务生成或伪造比对结论。服务重启后，当前内存中的任务历史会丢失；在执行器与正式任务存储接入前，这一资源仅用于固定前后端契约和联调。

## 模式

| `mode` | 含义 |
| --- | --- |
| `externalVersion` | 外规版本比对：对同一外规的新旧版本进行差异比对。 |
| `internalCoverage` | 内规覆盖度比对：以外规为基准，核查指定内规的覆盖情况。 |

## 创建任务

`POST /api/v1/policy-compare/tasks`

```json
{
  "mode": "externalVersion",
  "primaryObject": "上市公司信息披露管理办法 · 2026版",
  "secondaryObject": "上市公司信息披露管理办法 · 2024版",
  "scope": {
    "organizations": ["东方证券股份有限公司"],
    "businessScenes": ["信息披露"],
    "chapters": ["第八条"],
    "effectiveDateRange": ["2024-01-01", "2026-12-31"]
  },
  "attachmentFileIds": []
}
```

成功返回 `201`。当前返回的 `status` 固定为 `PENDING_EXECUTOR`，且 `result` 必为 `null`：它表示任务已被记录、正等待 task-runtime 的对应执行器上线，并不表示已完成。

```json
{
  "taskId": "PC-...",
  "mode": "externalVersion",
  "status": "PENDING_EXECUTOR",
  "primaryObject": "上市公司信息披露管理办法 · 2026版",
  "secondaryObject": "上市公司信息披露管理办法 · 2024版",
  "scope": { "organizations": [], "businessScenes": [], "chapters": [], "effectiveDateRange": [] },
  "createdAt": "2026-08-03T02:00:00Z",
  "updatedAt": "2026-08-03T02:00:00Z",
  "message": "比对任务已创建，等待执行器接入。",
  "result": null
}
```

参数不合法时返回 `400`：

```json
{
  "error": {
    "code": "POLICY_COMPARE_INVALID_REQUEST",
    "message": "核查模式仅支持 externalVersion 或 internalCoverage。"
  }
}
```

## 查询任务

- `GET /api/v1/policy-compare/tasks/{taskId}`：返回单个任务；找不到时返回 `404` 与 `POLICY_COMPARE_TASK_NOT_FOUND`。
- `GET /api/v1/policy-compare/tasks?mode=internalCoverage&page=1&pageSize=20`：按模式分页查询本进程生命周期内创建的任务。`mode` 可省略，`pageSize` 最大为 100。

列表返回：

```json
{
  "items": [],
  "page": 1,
  "pageSize": 20,
  "total": 0
}
```

## task-runtime 接入约定

task-runtime 增加 `external-version-compare` 与 `internal-coverage-compare`（名称以其最终契约为准）后，Java 仅替换 `PolicyCompareTaskService` 的执行分支：

1. 创建任务时转为 `QUEUED`，向 task-runtime 提交同一份已校验的任务输入；
2. 按 task-runtime 的 run 状态更新为 `RUNNING`、`COMPLETED`、`FAILED` 或 `CANCELLED`；
3. 只有 `COMPLETED` 才写入 `result`；任意非完成终态均不得读取或展示结果。

现有 URL、`mode` 与请求字段保持不变，前端不需要等待 Pi 完成后再迁移一次接口。
