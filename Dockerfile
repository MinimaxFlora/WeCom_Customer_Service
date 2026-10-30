# syntax=docker/dockerfile:1

# ---------- Build stage ----------
FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /workspace

COPY pom.xml .
RUN mvn -B -q dependency:go-offline

COPY src ./src
RUN mvn -B -DskipTests -Ddocker.build.skip=true -Ddockerfile.skip=true package

# ---------- Runtime stage ----------
FROM eclipse-temurin:21-jre-jammy

ENV TZ=Asia/Shanghai \
    JAVA_OPTS=""

RUN apt-get update \
    && apt-get install -y --no-install-recommends ffmpeg curl \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app
COPY --from=build /workspace/target/kn_ai_chat-0.0.1-SNAPSHOT.jar /app/app.jar
COPY LICENSE NOTICE /app/
RUN mkdir -p /app/data /app/uploads

EXPOSE 8081

HEALTHCHECK --interval=30s --timeout=5s --start-period=40s --retries=3 \
    CMD curl -fsS http://127.0.0.1:8081/admin/login >/dev/null || exit 1

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
