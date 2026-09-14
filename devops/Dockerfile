# 默认 target 是 app（最后一阶段）。
# 迁移镜像：docker build -f devops/Dockerfile --target migration
ARG INCLUDE_OTEL=false

FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
ARG INCLUDE_OTEL

COPY pom.xml .
COPY common/pom.xml common/pom.xml
COPY app/pom.xml app/pom.xml
COPY generator/pom.xml generator/pom.xml
COPY settings.xml /root/.m2/settings.xml

RUN mvn dependency:go-offline -B -pl app -am && \
    if [ "$INCLUDE_OTEL" = "true" ]; then \
      curl -L https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases/download/v2.26.1/opentelemetry-javaagent.jar -o /app/opentelemetry-javaagent.jar; \
    fi

COPY common/src common/src
COPY generator/src generator/src
COPY app/src app/src

RUN mvn package -Dmaven.test.skip=true -B -pl app -am

FROM arigaio/atlas:1.2.3 AS atlas

FROM alpine:3.20 AS migration
WORKDIR /app/db

RUN apk add --no-cache mariadb-client \
  && addgroup -S appgroup && adduser -S appuser -G appgroup

COPY --from=atlas /atlas /usr/local/bin/atlas
COPY --chown=appuser:appgroup db /app/db

USER appuser

ENTRYPOINT ["/app/db/migrate.sh"]
CMD ["prod", "apply"]

FROM eclipse-temurin:21-jre-alpine AS app
WORKDIR /app

RUN addgroup -S appgroup && adduser -S appuser -G appgroup

COPY --from=build /app/app/target/*.jar app.jar
COPY --from=build /app/opentelemetry-javaagent.ja[r] /app/

ENV JAVA_OPTS="-Xms256m -Xmx512m -Djava.security.egd=file:/dev/./urandom"

EXPOSE 8080

USER appuser

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
