ARG INCLUDE_OTEL=false

# 1. 构建阶段：容器内 Maven 打包
FROM maven:3.9-eclipse-temurin-21 AS build
ARG INCLUDE_OTEL
WORKDIR /app

# OpenTelemetry agent 与依赖无关，单独下载，避免 dependency:go-offline 把插件依赖也拉全。
RUN mkdir -p /app/otel && \
    if [ "$INCLUDE_OTEL" = "true" ]; then \
      curl -fL https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases/download/v2.26.1/opentelemetry-javaagent.jar -o /app/otel/opentelemetry-javaagent.jar; \
      if ! jar tf /app/otel/opentelemetry-javaagent.jar > /dev/null 2>&1; then echo 'OTel agent download failed or invalid jar'; exit 1; fi; \
    fi

COPY pom.xml .
COPY common/pom.xml common/pom.xml
COPY app/pom.xml app/pom.xml
COPY generator/pom.xml generator/pom.xml
# settings 放在全局配置，避免被 /root/.m2 的 cache mount 盖住。
COPY settings.xml /usr/share/maven/conf/settings.xml

# 复制所有模块源码
COPY common/src common/src
COPY generator/src generator/src
COPY app/src app/src

# id=m2 与发布工作流注入的 Actions cache 对齐。命中的依赖不再下载。
RUN --mount=type=cache,id=m2,target=/root/.m2,sharing=locked \
    mvn package -Dmaven.test.skip=true -B -pl app -am

# 2. 运行阶段
FROM eclipse-temurin:21-jre-alpine AS runtime
WORKDIR /app

# 创建非 root 用户，并提前准备持久化存储目录的写权限。
# 固定 uid/gid：k8s runAsNonRoot 只能校验数字 UID（USER appuser 会被拒绝）。
RUN addgroup -S -g 101 appgroup && adduser -S -u 100 -G appgroup appuser && \
    mkdir -p /app/data/storage && \
    chown -R appuser:appgroup /app/data

# 从构建阶段复制打好的 jar 包
COPY --from=build /app/app/target/*.jar app.jar
# 暂存目录始终存在；未启用 INCLUDE_OTEL 时复制空目录
COPY --from=build /app/otel/ /app/

# JVM 运行参数
ENV JAVA_OPTS="-Xms256m -Xmx512m -Djava.security.egd=file:/dev/./urandom"

# 暴露服务端口
EXPOSE 8080

# 切换到非 root 用户
USER 100

# 启动命令（agent 文件不存在时自动剔除 -javaagent 参数，避免 JVM 启动失败）
ENTRYPOINT ["sh", "-c", "if [ ! -f /app/opentelemetry-javaagent.jar ] && [ -n \"$JAVA_TOOL_OPTIONS\" ]; then export JAVA_TOOL_OPTIONS=$(echo \"$JAVA_TOOL_OPTIONS\" | sed 's/-javaagent:[^ ]* *//g'); fi; exec java $JAVA_OPTS -jar /app/app.jar"]
