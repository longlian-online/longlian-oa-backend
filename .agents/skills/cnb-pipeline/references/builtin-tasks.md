# CNB 内置任务参考

所有内置任务通过 `type` 字段指定，用 `options` 传参，用 `exports` 导出结果。

```yaml
- name: my-task
  type: <type>
  options:
    key: value
  # optionsFrom: options.yml     # 从文件加载 options
  exports:
    output_key: ENV_VAR_NAME
```

---

## 任务一览

| 类型 | 功能 | 关键参数 | 支持的触发事件 |
|------|------|----------|----------------|
| `cnb:resolve` / `cnb:await` | 多 Pipeline 协作（同事件内） | `key`, `data`(resolve) | 所有事件（不可在 endStages/failStages 中使用） |
| `cnb:apply` | 触发同仓库子流水线 | `event`, `configFrom`, `sync` | push, commit.add, branch.create, pull_request.target, pull_request.mergeable, tag_push, pull_request.merged, api_trigger, web_trigger, crontab, tag_deploy |
| `cnb:trigger` | 触发其他仓库流水线 | `slug`, `event`, `branch`, `env`, `sync` | 所有事件 |
| `cnb:read-file` | 读取文件为环境变量 | `filePath` | 所有事件 |
| `cnb:destroy-token` | 销毁 CNB_TOKEN | 无 | 所有事件 |
| `docker:cache` | 构建 Docker 缓存镜像 | `dockerfile`, `by`, `versionBy` | 所有事件 |
| `git:auto-merge` | 自动合并 PR | `mergeType`, `removeSourceBranch` | **仅 pull_request.mergeable** |
| `git:reviewer` | 添加/删除评审人 | `type`, `reviewers`, `count`, `reviewersConfig` | pull_request, pull_request.target, pull_request.update |
| `git:release` | 发布 Release | `tag`, `description`, `descriptionFromFile` | push, commit.add, branch.create, tag_push, pull_request.merged, api_trigger, web_trigger, tag_deploy |
| `git:issue-update` | 更新 Issue 状态/标签 | `state`, `label`, `when`, `fromFile` | 所有事件 |
| `git:pr-update` | 更新 PR 标签/标题 | `label`, `title` | 所有事件 |
| `git:pr-commit-message-preset` | 预设 PR 提交信息 | `message` | pull_request, pull_request.target, pull_request.update, pull_request.mergeable, pull_request.approved, pull_request.changes_requested |
| `testing:coverage` | 单测覆盖率上报 | `pattern`, `lang`, `lines`, `diffLines` | 所有事件（增量覆盖率仅 PR 事件） |
| `artifact:remove-tag` | 删除制品标签 | `name`, `tags`, `type` | push, commit.add, tag_push, tag_deploy, pull_request.merged, api_trigger, web_trigger, crontab, branch.create |
| `tapd:status-update` | 更新 TAPD 状态 | `status`, `type`, `when` | 所有事件 |
| `tapd:comment` | TAPD 评论 | `comment`, `type` | 所有事件 |
| `vscode:go` | 控制云开发环境可用时机 | 无 | **仅 vscode, branch.create, api_trigger, web_trigger** |
| `knowledge:update` | 更新知识库 | `include`, `exclude`, `chunkSize` | 所有事件 |
| `npc:go` | 执行 AI/NPC 任务 | `role`, `systemPrompt`, `userPrompt` | 所有事件 |

> **在线文档**：${CNB_WEB_PROTOCOL:-https}://docs.${CNB_WEB_HOST:-cnb.cool}/zh/build/internal-steps.md

---

## 事件限制详情

部分内置任务对触发事件有严格限制，在不受支持的事件下使用会导致构建失败或无效果：

### `git:auto-merge` — 仅 `pull_request.mergeable`

该任务在 PR 满足合并条件时自动合并，仅支持 `pull_request.mergeable` 事件。在其他事件下使用无意义。

### `vscode:go` — 仅 `vscode`, `branch.create`, `api_trigger`, `web_trigger`

该任务控制云开发环境的可用性，仅在云开发相关事件中有效。

### `git:reviewer` — 仅 PR 事件

仅支持 `pull_request`, `pull_request.target`, `pull_request.update` 事件。这些事件与 PR 上下文关联。

### `git:pr-commit-message-preset` — 仅 PR 事件

仅支持 PR 相关事件：`pull_request`, `pull_request.target`, `pull_request.update`, `pull_request.mergeable`, `pull_request.approved`, `pull_request.changes_requested`。

### `cnb:apply` — 限定事件

仅支持：`push`, `commit.add`, `branch.create`, `pull_request.target`, `pull_request.mergeable`, `tag_push`, `pull_request.merged`, `api_trigger`, `web_trigger`, `crontab`, `tag_deploy`。不支持 Issue、NPC、PR 评论等事件。

### `git:release` — 限定事件

仅支持：`push`, `commit.add`, `branch.create`, `tag_push`, `pull_request.merged`, `api_trigger`, `web_trigger`, `tag_deploy`。

### `artifact:remove-tag` — 限定事件

仅支持：`push`, `commit.add`, `tag_push`, `tag_deploy`, `pull_request.merged`, `api_trigger`, `web_trigger`, `crontab`, `branch.create`。

---

## 常用示例

### cnb:trigger（触发其他仓库）

```yaml
- name: trigger-deploy
  type: cnb:trigger
  options:
    slug: org/deploy-repo
    branch: main
    event: api_trigger
    env:
      VERSION: $VERSION
    sync: true                   # 同步等待完成
  exports:
    sn: BUILD_SN
```

### cnb:await / cnb:resolve（多 Pipeline 协作）

```yaml
# Pipeline A：完成后通知
- name: notify-ready
  type: cnb:resolve
  options:
    key: frontend-ready
    data: { version: $VERSION }

# Pipeline B：等待通知
- name: wait-frontend
  type: cnb:await
  options:
    key: frontend-ready
  exports:
    version: FRONTEND_VERSION
```

### git:release（发布 Release）

```yaml
- name: release
  type: git:release
  options:
    tag: v1.0.0
    description: "Release v1.0.0"
    # descriptionFromFile: CHANGELOG.md
  exports:
    version: RELEASE_VERSION
```

### git:auto-merge（自动合并 PR）

```yaml
# 必须在 pull_request.mergeable 事件下使用
- name: auto-merge
  type: git:auto-merge
  options:
    mergeType: squash
    removeSourceBranch: true
```

### testing:coverage（覆盖率上报）

```yaml
- name: coverage
  type: testing:coverage
  options:
    pattern: coverage/lcov.info
    lang: javascript
    lines: 80                    # 总覆盖率阈值
    diffLines: 90                # 增量覆盖率阈值
```

### docker:cache（构建缓存镜像）

```yaml
- name: cache-deps
  type: docker:cache
  options:
    dockerfile: Dockerfile.cache
    by: [package.json]
    versionBy: [package-lock.json]
```
