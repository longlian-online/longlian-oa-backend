# Maven 发版指南

本文说明项目基于 Maven Release Plugin 的版本更新、Git 标签和构件发布流程。

本文命令默认远程仓库名为 `origin`。如果本地使用其他远程名，请将命令中的
`origin` 替换为实际远程名。

## 发版配置

根目录 `pom.xml` 已配置：

- `maven-release-plugin`：负责准备版本、提交 POM、创建 Git 标签和推送变更。
- `versions-maven-plugin`：用于手动设置多模块项目版本。
- Git SCM：使用 GitHub SSH 地址推送提交和标签。
- 标签格式：`v<releaseVersion>`，例如 `v1.0.0`。

子模块继承根 POM 的版本和 SCM 配置，不要在子模块中重复配置 `<scm>`。

### 重要：标签和代码分支是两个独立的远程引用

Git 标签不会自动移动 `main`。`release:prepare` 只会在执行命令的当前分支上提交版本变更、创建标签并推送；它不会替当前分支创建或合并 Pull Request。

因此有两种受支持的流程：

1. **直接在 `main` 发版**：正式版本提交、`v<version>` 标签和下一个开发版本提交都会推送到 `main`，不需要额外合并。
2. **在发版分支发版**：`release:prepare` 完成后，必须将发版分支合并回 `main`。在合并完成前，可能出现远程标签已经存在，但远程 `main` 还不包含该标签所指向提交的情况。

`release:prepare` 的多次 Git 推送也不是一个原子操作。若网络或权限问题发生在其中一步，必须按照“发版完成检查”确认远程分支、标签和提交状态，不能只根据标签是否存在判断发版完成。

## 发版前检查

发版命令会提交 POM 并创建或推送标签，执行前确认：

```bash
git status --short
git fetch origin --prune
mvn -B validate -DskipTests
mvn -B clean verify
```

工作区必须干净。GitHub SSH 权限可以用下面的命令检查：

```bash
ssh -T git@github.com
```

同时确认目标标签不存在：

```bash
git ls-remote --tags origin refs/tags/v1.0.0
```

如果命令没有输出，表示远程目前没有该标签。

## 标准发版流程

### 1. 选择发版分支

如果仓库允许直接推送 `main`，推荐先同步 `main` 后直接在其上执行后续命令：

```bash
git switch main
git pull --ff-only origin main
```

如果仓库要求通过发版分支和 Pull Request 合并，则从最新的 `main` 创建工作分支，示例：

```bash
git switch -c build/release-1.0.3 origin/main
```

发版分支模式下，后续命令中的 `1.0.3`、`1.0.4-SNAPSHOT` 和 `v1.0.3` 按实际版本替换。

### 2. 预览发版变更

`release:prepare` 会把当前的 `SNAPSHOT` 版本改为正式版本，创建 Git 标签，
然后将工作区版本改为下一个开发版本。建议先使用 dry-run：

```bash
mvn -B release:prepare \
  -DdryRun=true \
  -DreleaseVersion=1.0.0 \
  -DdevelopmentVersion=1.0.1-SNAPSHOT \
  -Dtag=v1.0.0
```

预览完成后清理 Release Plugin 生成的状态文件：

```bash
mvn -B release:clean
```

### 3. 准备正式版本并创建标签

```bash
mvn -B release:prepare \
  -DreleaseVersion=1.0.0 \
  -DdevelopmentVersion=1.0.1-SNAPSHOT \
  -Dtag=v1.0.0
```

执行成功后，插件会完成以下操作：

1. 将正式版本写入所有相关 POM。
2. 提交正式版本变更。
3. 创建并推送 `v1.0.0` 标签。
4. 将当前分支 POM 更新为 `1.0.1-SNAPSHOT` 并提交。

`release:prepare` 已经包含版本更新和打标签；只需要这两项时不必执行
`release:perform`。

### 4. 发版分支合并回 main

只有在第 1 步选择了发版分支时执行本节。确认 `release:prepare` 成功后，先将分支推送到远程（如果插件已经推送成功则无需重复执行）：

```bash
git push -u origin build/release-1.0.3
```

然后创建以 `main` 为目标分支的 Pull Request，按仓库评审规则完成检查并合并。合并完成后再执行下一节的远程校验。不要因为 `v1.0.3` 标签已经存在就跳过这一步。

直接在 `main` 发版时没有这一步。

### 5. 发版完成检查

发版完成必须同时满足：远程标签存在、标签指向的正式版本提交已经进入远程 `main`，并且下一个开发版本提交位于预期分支。示例：

```bash
git fetch origin --prune --tags

# 确认远程 main 和标签都存在
git ls-remote --heads origin main
git ls-remote --tags origin "refs/tags/v1.0.3^{}"

# 返回码为 0 表示标签提交已包含在远程 main 中
git merge-base --is-ancestor "v1.0.3^{commit}" origin/main
```

如果最后一条命令返回非 0，说明标签已经推送但代码尚未合并到远程 `main`；发版分支模式下应回到上一节完成合并。最后确认 GitHub Actions 和必要的构件发布任务均成功。

### 6. 发布构件（可选）

如果需要将构件部署到 Maven 仓库，在 `release:prepare` 成功后执行：

```bash
mvn -B release:perform
```

该命令会检出正式版本标签并执行 `deploy`。当前项目 POM 未配置
`<distributionManagement>`，因此执行前必须在 POM 中配置可写的 Maven 仓库，
并在 Maven `settings.xml` 中为相同的仓库 `id` 配置认证信息。仅配置
`settings.xml` 的 `<server>` 不能提供部署地址。

如果不希望修改 POM，也可以向 `release:perform` 传入 Maven Deploy Plugin 支持的
`altDeploymentRepository` 参数。参数格式以当前使用的 Maven Deploy Plugin 版本为准，
示例：

```bash
mvn -B release:perform \
  -Darguments="-DaltDeploymentRepository=internal::https://repo.example.com/releases"
```

## 仅手动修改版本

不使用 Release Plugin 时，可以用 `versions-maven-plugin` 修改所有模块版本：

```bash
mvn -B versions:set \
  -DnewVersion=1.0.0 \
  -DprocessAllModules=true
mvn -B versions:commit
```

然后由 Git 完成提交和打标签：

```bash
git add pom.xml app/pom.xml common/pom.xml generator/pom.xml
git commit -m "build: set release version 1.0.0"
git tag v1.0.0
git push origin HEAD --follow-tags
```

这种方式不会自动生成下一个 `SNAPSHOT` 版本，也不会执行 Maven 构件发布。

## 常见故障案例：tag 已推送但 main 未更新

例如 `v1.0.1` 在 `build/release-1.0.1` 上完成了 `release:prepare`，标签和下一个开发版本提交随后都已推送到该分支；但直到约 11 小时 25 分钟后，该分支才合并到 `main`。这段时间内查看标签会误以为发版完成，实际上远程 `main` 还不能代表该版本代码。

排查时应按时间顺序检查：

1. 正式版本提交是否已经推送。
2. 标签是否指向该正式版本提交。
3. 下一个开发版本提交是否已经推送到执行发版的分支。
4. 发版分支是否已经合并到远程 `main`。
5. 标签提交是否满足上一节的 `merge-base --is-ancestor` 校验。

根因通常不是 Maven 丢失提交，而是把“推送标签”和“合并代码分支”当成了同一个动作。

## 最近三个版本时间线

以下时间均为北京时间，依据 Git 提交记录、远程引用和 GitHub Actions 启动时间核对（截至 2026-09-17）：

| 版本 | 正式版本提交 | 标签任务 | 下一个开发版本提交 | 进入远程 `main` 的时间 | 结论 |
| --- | --- | --- | --- | --- | --- |
| `v1.0.2` | `1b48872`，09-17 13:26:42 | 13:27:37 | `a82338d`，13:28:09 | 13:26:42 | 在 `main` 发版，无合并间隔 |
| `v1.0.1` | `b89a604`，09-17 01:47:23 | 01:47:56 | `20f7336`，01:48:56（发版分支） | 13:13:01 | 标签先于 `main` 约 11 小时 25 分钟 |
| `v1.0.0` | `35c2e6d`，09-16 19:59:53 | 20:00:05 | `6f7f9c8`，20:00:03 | 19:59:53 | 在 `main` 发版，无合并间隔 |

这三次记录表明：问题只出现在发版分支没有立即合并回 `main` 的流程中；直接在 `main` 执行插件时，标签和代码提交可以连续推送，但仍应执行上一节的远程校验。

## 失败处理

如果 `release:prepare` 在本地失败且尚未推送正式标签，可以尝试：

```bash
mvn -B release:rollback
mvn -B release:clean
```

如果正式提交或标签已经推送，不要直接执行回滚；先确认远程提交和标签状态，
再根据实际情况修复版本或创建新的修复版本。

## 提交和 Pull Request

提交消息遵循 [CONTRIBUTING.md](CONTRIBUTING.md)：

```bash
git add docs/release.md
git commit -m "docs(release): clarify tag and branch sync"
git push -u origin HEAD
```

如果使用发版分支，Pull Request 的目标分支应为 `main`，而不是标签对应的旧发布分支。Pull Request 使用 [pull_request_template.md](pull_request_template.md)，建议标题为：

```text
docs(release): clarify tag and branch sync
```

PR 描述至少说明发版配置、标准命令、分支合并和远程校验方式，并在合并前确认 Maven 校验和 CI 检查结果。
