# Runtime Stage
# We use debian:bookworm-slim because the binary is linked against glibc (from the build stage),
# which makes it incompatible with Alpine (musl) unless we did complex static linking.
# Bookworm-slim is stable, multi-arch, and relatively small (~75MB).
FROM debian:bookworm-slim

# Install wget for healthcheck
RUN apt-get update && apt-get install -y wget && rm -rf /var/lib/apt/lists/*

# Create non-root user (UID/GID 65532) and ensure permissions for Docker socket
# - 'root' group (0): often owns /var/run/docker.sock
# - 'daemon' group (1): historically used for docker in some setups
RUN groupadd -g 65532 nonroot && \
    useradd -u 65532 -g nonroot -s /bin/false nonroot && \
    usermod -aG root,daemon nonroot

WORKDIR /app

# Copy the native executable
# Expects the binary to be in the build context (built externally)
COPY --chown=nonroot:nonroot porthole porthole

# Copy config templates
COPY --chown=nonroot:nonroot config/ /app/config/

# Switch to non-root user
USER nonroot

# Expose the port
EXPOSE 9753

# Health check using Spring Actuator
HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:9753/actuator/health || exit 1

# Run the native application
ENTRYPOINT ["./porthole", "--spring.config.additional-location=file:/app/config/"]
