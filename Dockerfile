## ----- Multi-stage build for minimal image size and fast startup -----

# Stage 1: Build
FROM eclipse-temurin:21-jdk AS builder
WORKDIR /build
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
# Download dependencies first (cached layer — only invalidated when pom.xml changes)
RUN chmod +x mvnw && ./mvnw dependency:go-offline -q
COPY src/ src/
RUN ./mvnw clean package -DskipTests -q

# Stage 2: Run — JRE only, ~50% smaller image
FROM eclipse-temurin:21-jre
WORKDIR /app

# Install postgresql-client and default-mysql-client for database backups/restores
RUN apt-get update && \
    apt-get install -y postgresql-client default-mysql-client && \
    rm -rf /var/lib/apt/lists/*

# Copy only the built jar from the builder stage
COPY --from=builder /build/target/moneymanager-0.0.1-SNAPSHOT.jar app.jar

# Render injects PORT env var; expose it for documentation
EXPOSE ${PORT:-8080}

# JVM tuning for containerised workloads:
#   -XX:+UseContainerSupport     — respect cgroup memory/CPU limits
#   -XX:MaxRAMPercentage=75.0    — use up to 75% of container RAM for heap
#   -Djava.security.egd          — faster SecureRandom (avoids /dev/random blocking)
#   -Dspring.jmx.enabled=false   — skip JMX (not useful in containers)
CMD ["sh", "-c", "java \
  -XX:+UseContainerSupport \
  -XX:MaxRAMPercentage=75.0 \
  -Djava.security.egd=file:/dev/./urandom \
  -Dspring.jmx.enabled=false \
  -jar app.jar \
  --server.port=${PORT:-8080}"]
