# Multi-stage Dockerfile for FastBills Backend
# Stage 1: Build JAR
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /workspace

# Copy Maven wrapper & POM
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw && sh mvnw dependency:go-offline -B

# Copy source code and package
COPY src/ src/
RUN sh mvnw clean package -DskipTests -B

# Stage 2: Minimal Production JRE
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Create non-root user and pre-create storage directories with proper ownership
RUN addgroup -S fastbills && adduser -S fastbills -G fastbills \
    && mkdir -p /app/data/storage \
    && chown -R fastbills:fastbills /app

# Copy compiled JAR from builder stage and assign ownership
COPY --from=builder --chown=fastbills:fastbills /workspace/target/fastbills-api-0.0.1-SNAPSHOT.jar app.jar

USER fastbills

# Dynamic port binding and storage directory defaults
ENV PORT=8080
ENV FASTBILLS_STORAGE_LOCAL_DIR=/app/data/storage
EXPOSE 8080

ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75", "-jar", "app.jar"]
