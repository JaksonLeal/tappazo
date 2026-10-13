# ========================================================
# Tappazo Backend - Multi-Stage Dockerfile (Java 21)
# ========================================================

# --------------------------------------------------------
# Stage 1: Build stage
# --------------------------------------------------------
FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /build

# Copy Maven wrapper and descriptor to leverage Docker caching
COPY pom.xml mvnw ./
COPY .mvn .mvn

RUN chmod +x ./mvnw && ./mvnw dependency:go-offline -B || true

# Copy source code and build production artifact
COPY src ./src
RUN ./mvnw clean package -DskipTests -B

# --------------------------------------------------------
# Stage 2: Production runtime stage
# --------------------------------------------------------
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Run as dedicated unprivileged user for security
RUN addgroup -S tappazo && adduser -S tappazo -G tappazo

# Copy packaged JAR from build stage
COPY --from=builder /build/target/*.jar app.jar
RUN chown -R tappazo:tappazo /app

USER tappazo:tappazo

# Expose Spring Boot HTTP port
EXPOSE 8080

# Configure container-aware JVM settings
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -XX:InitialRAMPercentage=50.0 -Djava.security.egd=file:/dev/./urandom"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
