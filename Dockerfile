# Stage 1: Build with dependency caching
FROM maven:3.9.6-eclipse-temurin-21-alpine AS build

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

# Build the application
WORKDIR /app/server
RUN mvn clean package -DskipTests -Pbuild-client

# Stage 2: Runtime
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Copy the built artifact from build stage
COPY --from=build /app/server/target/porthole-0.0.1-SNAPSHOT.jar app.jar

# Expose the port
EXPOSE 8080

# Run the application
CMD ["java", "-jar", "app.jar"]
