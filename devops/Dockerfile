ARG INCLUDE_OTEL=false

# 1. 构建阶段：容器内 Maven 打包
FROM maven:3.9-eclipse-temurin-21 AS build
ARG INCLUDE_OTEL
WORKDIR /app

# 复制各级 pom.xml，下载依赖
COPY pom.xml .
COPY common/pom.xml common/pom.xml
COPY app/pom.xml app/pom.xml
COPY generator/pom.xml generator/pom.xml
COPY settings.xml /root/.m2/settings.xml

# 下载依赖及 OpenTelemetry Java Agent（仅 INCLUDE_OTEL=true 时下载）
RUN mvn dependency:go-offline -B -pl app -am && \
    mkdir -p /app/otel && \
    if [ "$INCLUDE_OTEL" = "true" ]; then \
      curl -fL https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases/download/v2.26.1/opentelemetry-javaagent.jar -o /app/otel/opentelemetry-javaagent.jar; \
      if ! jar tf /app/otel/opentelemetry-javaagent.jar > /dev/null 2>&1; then echo 'OTel agent download failed or invalid jar'; exit 1; fi; \
    fi

# 复制所有模块源码
COPY common/src common/src
COPY generator/src generator/src
COPY app/src app/src

# 构建 app 模块
RUN mvn package -Dmaven.test.skip=true -B -pl app -am

# 2. 运行阶段
FROM eclipse-temurin:21-jre-alpine AS runtime
WORKDIR /app

# 创建非 root 用户
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

# 从构建阶段复制打好的 jar 包
COPY --from=build /app/app/target/*.jar app.jar
# 暂存目录始终存在；未启用 INCLUDE_OTEL 时复制空目录
COPY --from=build /app/otel/ /app/

# JVM 运行参数
ENV JAVA_OPTS="-Xms256m -Xmx512m -Djava.security.egd=file:/dev/./urandom"

# 暴露服务端口
EXPOSE 8080

# 切换到非 root 用户
USER appuser

# 启动命令（agent 文件不存在时自动剔除 -javaagent 参数，避免 JVM 启动失败）
ENTRYPOINT ["sh", "-c", "if [ ! -f /app/opentelemetry-javaagent.jar ] && [ -n \"$JAVA_TOOL_OPTIONS\" ]; then export JAVA_TOOL_OPTIONS=$(echo \"$JAVA_TOOL_OPTIONS\" | sed 's/-javaagent:[^ ]* *//g'); fi; exec java $JAVA_OPTS -jar /app/app.jar"]
