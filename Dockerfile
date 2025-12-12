# Client Build Stage
FROM node:24-alpine AS client-build
WORKDIR /app/client
COPY client/package*.json ./
RUN npm ci
COPY client/ ./
RUN npm run build

# Server Build Stage (Native)
# Uses standard GraalVM image (glibc-based) which supports both AMD64 and ARM64
FROM ghcr.io/graalvm/native-image-community:25 AS server-build
WORKDIR /app/server

# Install Maven
RUN microdnf install -y maven && microdnf clean all

# Copy pom.xml and download dependencies
COPY server/pom.xml .
RUN mvn dependency:go-offline -B

# Copy server source
COPY server/src ./src

# Copy built client from previous stage
# The 'copy-client' profile expects ../client/dist relative to the server dir
COPY --from=client-build /app/client/dist ../client/dist

# Build native image
# -Pnative activates the native profile
# -Pcopy-client activates the maven-resources-plugin to copy the client dist
# 'package' phase ensures 'prepare-package' (copy-client) runs before native compilation
# 'native:compile' needs to be called explicitly as it's not bound to package phase
RUN mvn -Pnative,copy-client package native:compile -DskipTests -B

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

# Copy the native executable from build stage
# Note: The artifact name from native-maven-plugin defaults to project.artifactId (porthole)
COPY --from=server-build --chown=nonroot:nonroot /app/server/target/porthole porthole

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
