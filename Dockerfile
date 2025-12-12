# Multi-stage build for GraalVM native image (multi-platform)

# Stage 1: Build native executable
FROM ghcr.io/graalvm/native-image-community:25-muslib AS build

WORKDIR /app

# Copy Maven wrapper and pom
COPY server/.mvn ./.mvn
COPY server/mvnw ./
COPY server/pom.xml ./
RUN chmod +x mvnw

# Download dependencies (cached layer)
RUN ./mvnw dependency:go-offline -B

# Copy pre-built client dist
COPY client/dist ./client-dist

# Copy server source
COPY server/src ./src

# Build native image
RUN mkdir -p target/classes/static && \
    cp -r client-dist/* target/classes/static/ && \
    ./mvnw -Pnative native:compile -DskipTests -B

# Stage 2: Runtime
FROM alpine:3.21

# Create non-root user
RUN addgroup -g 1000 porthole && \
    adduser -u 1000 -G porthole -s /bin/sh -D porthole

WORKDIR /app

# Copy the native executable from build stage
COPY --from=build --chown=porthole:porthole /app/target/porthole porthole

# Copy config templates for user overrides
COPY --chown=porthole:porthole config/ /app/config/

# Switch to non-root user
USER porthole

# Expose the port
EXPOSE 9753

# Health check using Spring Actuator
HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:9753/actuator/health || exit 1

# Run the native application with external config
CMD ["./porthole", "--spring.config.additional-location=file:/app/config/"]
