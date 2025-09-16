# Multi-stage Dockerfile for building all services
FROM maven:3.9.6-eclipse-temurin-21 AS builder


# Set working directory
WORKDIR /app

# Copy parent POM
#COPY pom.xml .

# Copy common module
#COPY common/ common/

# Copy fraud-check-service
#COPY fraud-check-service/ fraud-check-service/

# Copy project root
COPY . .

# Build the application with Maven cache
# Note .m2 cache only works with docker buildkit builds
# Ensure DOCKER_BUILDKIT=1 is set
# DOCKER_BUILDKIT=1 docker build -f fraud-check-service -t fraud-check-service .
RUN --mount=type=cache,target=/root/.m2 \
  mvn clean package -pl fraud-check-system -am -DskipTests

# Runtime stage
FROM eclipse-temurin:21-jre-alpine

# Install curl for health checks
RUN apk add --no-cache curl

WORKDIR /app

# Copy the JAR file
COPY --from=builder /app/fraud-check-system/target/*.jar app.jar

# Download OpenTelemetry Java agent
ADD https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases/latest/download/opentelemetry-javaagent.jar opentelemetry-javaagent.jar

# Create logs directory
RUN mkdir -p logs

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:8083/actuator/health || exit 1

# Environment variables
ENV OTEL_SERVICE_NAME=fraud-check-system
ENV OTEL_TRACES_EXPORTER=otlp
ENV OTEL_METRICS_EXPORTER=otlp
ENV OTEL_LOGS_EXPORTER=otlp

EXPOSE 8083

ENTRYPOINT ["java", "-javaagent:opentelemetry-javaagent.jar", "-jar", "app.jar"]