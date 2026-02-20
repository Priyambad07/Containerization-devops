# Stage 1: Build
FROM maven:3.8-openjdk-11 AS builder
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

# Stage 2: Runtime
FROM openjdk:11-jre-slim
WORKDIR /app
# Copy only the built JAR, not source code or Maven
COPY --from=builder /app/target/app.jar app.jar

# Security: Run as non-root user
RUN useradd -m myuser
USER myuser

CMD ["java", "-jar", "app.jar"]
