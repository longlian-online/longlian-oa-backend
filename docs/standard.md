# 项目规范

## 命名
对象、方法、文件等命名，可以写的详细，可读性优先

## Controller
1. 只做 DTO 的校验，BO 组装和 VO 的返回
2. 用户会话信息应该在 Controller 层取出并传入Service（对应 Service #2）

## Service 

1. Service 层入参和出参须使用 BO 类，入参：XXXXParamsBO,出参：XXXXResultBO
2. Service 层方法应保持环境无关性。凡涉及用户会话信息（如 userId、orgId），应通过方法参数显式传递，严禁在 Service 内部直接访问 ThreadLocal 或 Session 上下文。此举旨在确保业务逻辑在 Controller、定时任务 (Cron)、消息处理 (MQ) 及单元测试 等多场景下的高度可复用性。

## Mapper
1. 数据库操作调用 Mapper 方法操作（参考https://baomidou.com/guides/data-interface/#mapper-interface），以 Lambda 操作优先，以保证类型安全
