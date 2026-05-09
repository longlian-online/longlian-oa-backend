# 提交指南

## 模板

```
<type>(<scope>): <subject>

<body>

<footer>
```

只要求必填：type: subject
其余可选，根据复杂程度而定

## type 类型

- feat：新功能、新需求
- fix：修复 bug
- docs：仅文档修改
- style：格式调整（不影响代码逻辑，如空格、分号、换行）
- refactor：重构（既不是新功能也不是改 bug）
- perf：性能优化
- test：添加 / 修改测试用例
- build：构建系统、依赖、打包配置
- ci：CI/CD 配置
- chore：杂项（构建、工具、不修改 src/test 的其他改动）
- revert：回滚某次提交

## scope（可选）

说明影响范围，用模块名 / 功能名：

user、order、pay、db、api、login、router 等

## subject

简短描述，不超过 50 字符

动词开头，不用句号

英文小写开头，中文直接写

## body（可选）

解释为什么改、做了什么、注意点

每行不超过 72 字符

## footer（可选）

关联 Issue：Closes #123

破坏性变更：BREAKING CHANGE: xxxx
