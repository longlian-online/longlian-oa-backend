# SQL 日志

MyBatis 使用 SLF4J，默认 INFO 级别不输出 SQL 和绑定参数。
开发排查时显式设置 `LOGGING_LEVEL_ONLINE_LONGLIAN_APP_MAPPER=DEBUG`，
或在本地配置中设置 `logging.level.online.longlian.app.mapper: DEBUG`。

生产环境保持 INFO；临时启用 DEBUG 后应及时恢复。不要启用 StdOutImpl，
该实现绕过应用日志级别控制，查询和写入参数可能包含 token、密码哈希等敏感信息。
