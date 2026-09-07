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

# Run as non-root user
RUN addgroup -S fastbills && adduser -S fastbills -G fastbills
USER fastbills

# Copy compiled JAR from builder stage
COPY --from=builder /workspace/target/fastbills-api-0.0.1-SNAPSHOT.jar app.jar

# Dynamic port binding for Render/Heroku/AWS (defaults to 8080)
ENV PORT=8080
EXPOSE 8080

ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75", "-jar", "app.jar"]
