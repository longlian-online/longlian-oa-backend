# 默认 target 是 app（最后一阶段）。
# 迁移镜像：docker build -f devops/Dockerfile --target migration
ARG INCLUDE_OTEL=false

# 1. 构建阶段：容器内 Maven 打包
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
ARG INCLUDE_OTEL

# 复制各级 pom.xml，下载依赖
COPY pom.xml .
COPY common/pom.xml common/pom.xml
COPY app/pom.xml app/pom.xml
COPY generator/pom.xml generator/pom.xml
COPY settings.xml /root/.m2/settings.xml

# 下载依赖及 OpenTelemetry Java Agent（仅 INCLUDE_OTEL=true 时下载）
RUN mvn dependency:go-offline -B -pl app -am && \
    if [ "$INCLUDE_OTEL" = "true" ]; then \
      curl -L https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases/download/v2.26.1/opentelemetry-javaagent.jar -o /app/opentelemetry-javaagent.jar; \
    fi

# 复制所有模块源码
COPY common/src common/src
COPY generator/src generator/src
COPY app/src app/src

# 构建 app 模块
RUN mvn package -Dmaven.test.skip=true -B -pl app -am

# 2. 迁移镜像：Atlas CLI + schema.sql，无需 JVM
FROM arigaio/atlas:1.2.3 AS atlas

FROM alpine:3.20 AS migration
WORKDIR /app/db

# mariadb-client 用于同实例自动创建 {db}_atlas 和导入种子
RUN apk add --no-cache mariadb-client=10.11.18-r0 \
  && addgroup -S appgroup && adduser -S appuser -G appgroup

COPY --from=atlas /atlas /usr/local/bin/atlas
COPY --chown=appuser:appgroup db /app/db

USER appuser

ENTRYPOINT ["/app/db/migrate.sh"]
CMD ["prod", "apply"]

# 3. 运行阶段
FROM eclipse-temurin:21-jre-alpine AS app
WORKDIR /app

# 创建非 root 用户
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

# 从构建阶段复制打好的 jar 包
COPY --from=build /app/app/target/*.jar app.jar
# 按需复制 OTel agent（glob 匹配，文件不存在时静默跳过）
COPY --from=build /app/opentelemetry-javaagent.ja[r] /app/

# JVM 运行参数
ENV JAVA_OPTS="-Xms256m -Xmx512m -Djava.security.egd=file:/dev/./urandom"

# 暴露服务端口
EXPOSE 8080

# 切换到非 root 用户
USER appuser

# 启动命令（exec 形式确保信号正确转发）
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
