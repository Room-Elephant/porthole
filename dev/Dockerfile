# Stage 1: Build with dependency caching
FROM maven:3.9-eclipse-temurin-25-alpine AS build

WORKDIR /app

# Copy dependency files first (these change less frequently)
COPY server/pom.xml ./server/
COPY client/package.json client/package-lock.json ./client/

# Download Maven dependencies (this layer will be cached)
WORKDIR /app/server
RUN mvn dependency:go-offline -B

# Install Node.js and npm in the build stage
RUN apk add --no-cache nodejs npm

# Install npm dependencies (this layer will be cached)
WORKDIR /app/client
RUN npm ci

# Copy source code
WORKDIR /app
COPY server/src ./server/src
COPY client ./client

# Build the frontend
WORKDIR /app/client
RUN npm run build

# Build the backend (without frontend profile - we built it manually above)
WORKDIR /app/server
RUN mvn clean package -DskipTests

# Copy frontend dist into the JAR's static resources
RUN mkdir -p target/classes/static && \
    cp -r ../client/dist/* target/classes/static/ && \
    cd target && \
    jar uf porthole-0.0.1-SNAPSHOT.jar -C classes static

# Stage 2: Runtime
FROM eclipse-temurin:25-jre-alpine

WORKDIR /app

# Copy the built artifact from build stage
COPY --from=build /app/server/target/porthole-0.0.1-SNAPSHOT.jar app.jar

# Copy config templates for user overrides
COPY docker/templates/ /app/config/


# Expose the port
EXPOSE 9753

# Health check using Spring Actuator
HEALTHCHECK --interval=30s --timeout=3s --start-period=10s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:9753/actuator/health || exit 1

# Run the application with external config
CMD ["java", "-jar", "app.jar", "--spring.config.additional-location=file:/app/config/"]

