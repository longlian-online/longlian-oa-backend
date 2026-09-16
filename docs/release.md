# Maven 发版指南

本文说明项目基于 Maven Release Plugin 的版本更新、Git 标签和构件发布流程。

## 发版配置

根目录 `pom.xml` 已配置：

- `maven-release-plugin`：负责准备版本、提交 POM、创建 Git 标签和推送变更。
- `versions-maven-plugin`：用于手动设置多模块项目版本。
- Git SCM：使用 GitHub SSH 地址推送提交和标签。
- 标签格式：`v<releaseVersion>`，例如 `v1.0.0`。

子模块继承根 POM 的版本和 SCM 配置，不要在子模块中重复配置 `<scm>`。

## 发版前检查

发版命令会提交 POM 并创建或推送标签，执行前确认：

```bash
git status --short
git fetch github --prune
mvn -B validate -DskipTests
mvn -B clean verify
```

工作区必须干净。GitHub SSH 权限可以用下面的命令检查：

```bash
ssh -T git@github.com
```

同时确认目标标签不存在：

```bash
git ls-remote --tags github refs/tags/v1.0.0
```

如果命令没有输出，表示远程目前没有该标签。

## 标准发版流程

### 1. 创建发版分支

从目标发布分支创建工作分支，示例：

```bash
git switch -c build/release-1.0.0 github/release/v1.0.0
```

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

### 4. 发布构件（可选）

如果需要将构件部署到 Maven 仓库，在 `release:prepare` 成功后执行：

```bash
mvn -B release:perform
```

该命令会检出正式版本标签并执行 `deploy`。执行前必须在 Maven `settings.xml`
或项目的 `<distributionManagement>` 中配置可写的 Maven 仓库及认证信息。

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
git push github HEAD --follow-tags
```

这种方式不会自动生成下一个 `SNAPSHOT` 版本，也不会执行 Maven 构件发布。

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
git commit -m "docs: add maven release guide"
git push -u github HEAD
```

Pull Request 使用 [pull_request_template.md](pull_request_template.md)，目标分支
为 `release/v1.0.0`。建议标题为：

```text
docs: add maven release guide
```

PR 描述至少说明发版配置、标准命令、仅版本更新命令以及失败处理方式，并在
合并前确认 Maven 校验和 CI 检查结果。
