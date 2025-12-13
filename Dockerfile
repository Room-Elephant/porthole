# Runtime Stage
# We use debian:bookworm-slim because the binary is linked against glibc (from the build stage),
# which makes it incompatible with Alpine (musl) unless we did complex static linking.
# Bookworm-slim is stable, multi-arch, and relatively small (~75MB).
FROM debian:bookworm-slim

# Install wget (healthcheck) and gosu (for privilege dropping)
RUN apt-get update && apt-get install -y wget gosu && rm -rf /var/lib/apt/lists/*

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

# Copy and setup entrypoint
COPY --chmod=755 entrypoint.sh /app/entrypoint.sh

# Expose the port
EXPOSE 9753

# Health check using Spring Actuator
HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:9753/actuator/health || exit 1

# Run via entrypoint script which handles permissions and then switches user
ENTRYPOINT ["/app/entrypoint.sh", "./porthole", "--spring.config.additional-location=file:/app/config/"]
