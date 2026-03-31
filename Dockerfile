# 1. 构建阶段：容器内 Maven 打包
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

# 复制各级 pom.xml，下载依赖
COPY pom.xml .
COPY app/pom.xml app/pom.xml
COPY generator/pom.xml generator/pom.xml

# 下载 app 模块及其父模块的所有依赖
RUN mvn dependency:go-offline -B -pl app -am

# 复制 app 模块源码
COPY app/src app/src

# 构建 app 模块
RUN mvn package -DskipTests -B -pl app -am

# 2. 运行阶段
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# 从构建阶段复制打好的 jar 包
COPY --from=build /app/app/target/*.jar app.jar

# 复制生产环境配置文件
COPY app/src/main/resources/application-prod.yml application-prod.yml

# JVM 运行参数
ENV JAVA_OPTS="-Xms256m -Xmx512m -Djava.security.egd=file:/dev/./urandom"

# 暴露服务端口
EXPOSE 8080

# 启动命令
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar --spring.profiles.active=prod --spring.config.location=classpath:/,file:/app/application-prod.yml"]
