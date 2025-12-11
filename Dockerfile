# Dockerfile for CI/production builds
# Uses pre-built JAR from Maven build with copy-client profile

FROM eclipse-temurin:25-jre-alpine

# Create non-root user
RUN addgroup -g 1000 porthole && \
    adduser -u 1000 -G porthole -s /bin/sh -D porthole

WORKDIR /app

# Copy the pre-built JAR from Maven build
COPY --chown=porthole:porthole server/target/porthole-0.0.1-SNAPSHOT.jar app.jar

# Copy config templates for user overrides
COPY --chown=porthole:porthole config/ /app/config/

# Switch to non-root user
USER porthole

# Expose the port
EXPOSE 9753

# Health check using Spring Actuator
HEALTHCHECK --interval=30s --timeout=3s --start-period=10s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:9753/actuator/health || exit 1

# Run the application with external config
CMD ["java", "-jar", "app.jar", "--spring.config.additional-location=file:/app/config/"]
