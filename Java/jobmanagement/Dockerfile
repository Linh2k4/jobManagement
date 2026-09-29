# syntax=docker/dockerfile:1.7

# ============================================================
# Stage 1: Build stage
# ============================================================
# Use the Debian-based builder: it ships with the full system CA bundle, which
# avoids TLS-handshake failures that can occur with Alpine-based Java images.
FROM maven:3.9-eclipse-temurin-21 AS builder

WORKDIR /app

# Keep the certificate store current for connections to Maven Central.
RUN apt-get update \
    && apt-get install -y --no-install-recommends ca-certificates \
    && update-ca-certificates \
    && rm -rf /var/lib/apt/lists/*

# Copy root pom and module poms for dependency caching
COPY pom.xml .
COPY common/pom.xml common/
COPY model/pom.xml model/
COPY storage-db/pom.xml storage-db/
COPY cache/pom.xml cache/
COPY storage-minio/pom.xml storage-minio/
COPY iam/pom.xml iam/
COPY api/pom.xml api/

# Cache Maven downloads between builds.  Do not hide a dependency-resolution
# error: failing here gives a clear, early build failure.
RUN --mount=type=cache,target=/root/.m2 \
    mvn -B -ntp -Dmaven.wagon.http.retryHandler.count=5 dependency:go-offline

# Copy all sources and build the package
COPY common/src common/src
COPY model/src model/src
COPY storage-db/src storage-db/src
COPY cache/src cache/src
COPY storage-minio/src storage-minio/src
COPY iam/src iam/src
COPY api/src api/src

RUN --mount=type=cache,target=/root/.m2 \
    mvn -B -ntp -Dmaven.wagon.http.retryHandler.count=5 -DskipTests clean package

# ============================================================
# Stage 2: Runtime stage
# ============================================================
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Install curl for healthcheck
RUN apk add --no-cache curl

# Create non-root user for security
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

# Copy built artifact from builder stage
COPY --from=builder /app/api/target/api-1.0.0.jar app.jar

RUN chown -R appuser:appgroup /app

USER appuser

EXPOSE 8080

HEALTHCHECK --interval=15s --timeout=5s --start-period=30s --retries=3 \
  CMD curl -f http://localhost:8080/v3/api-docs || exit 1

ENTRYPOINT ["java", "-Djava.security.egd=file:/dev/./urandom", "-jar", "app.jar"]
