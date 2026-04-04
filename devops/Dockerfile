# 1. 构建阶段：容器内 Maven 打包
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

# 复制各级 pom.xml，下载依赖
COPY pom.xml .
COPY app/pom.xml app/pom.xml
COPY generator/pom.xml generator/pom.xml
COPY settings.xml /root/.m2/settings.xml

# 下载 app 模块及其父模块的所有依赖
RUN mvn dependency:go-offline -B -pl app -am

# 复制 app 模块源码
COPY app/src app/src

# 构建 app 模块
RUN mvn package -DskipTests -B -pl app -am

# 下载 OpenTelemetry Java Agent
RUN curl -L https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases/download/v2.26.1/opentelemetry-javaagent.jar -o /app/opentelemetry-javaagent.jar

# 2. 运行阶段
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# 从构建阶段复制打好的 jar 包和 OpenTelemetry agent
COPY --from=build /app/app/target/*.jar app.jar
COPY --from=build /app/opentelemetry-javaagent.jar opentelemetry-javaagent.jar

# JVM 运行参数（包含 OpenTelemetry Java Agent）
ENV JAVA_OPTS="-Xms256m -Xmx512m -Djava.security.egd=file:/dev/./urandom -javaagent:/app/opentelemetry-javaagent.jar"

# 暴露服务端口
EXPOSE 8080

# 启动命令
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]
