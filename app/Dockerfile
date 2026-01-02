# 使用轻量化 JDK 21 Alpine 镜像
FROM eclipse-temurin:21-jdk-alpine

# 设置工作目录为 /app
WORKDIR /app

# 复制 jar 包和配置文件
COPY app-0.0.1-SNAPSHOT.jar app.jar
COPY application-prod.yml application-prod.yml

# JVM 参数
ENV JAVA_OPTS="-Xms256m -Xmx512m -Dspring.profiles.active=prod -Djava.security.egd=file:/dev/./urandom"

EXPOSE 8080

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar --spring.config.location=/app/application-prod.yml"]
