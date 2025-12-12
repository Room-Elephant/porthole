# Client Build Stage
FROM node:24-alpine AS client-build
WORKDIR /app/client
COPY client/package*.json ./
RUN npm ci
COPY client/ ./
RUN npm run build

# Server Build Stage (Native)
FROM ghcr.io/graalvm/native-image-community:25-muslib AS server-build
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
# -Pcopy-client activtes the maven-resources-plugin to copy the client dist
# 'package' phase ensures 'prepare-package' (copy-client) runs before native compilation
RUN mvn -Pnative,copy-client package -DskipTests -B

# Runtime Stage
FROM alpine:3.21

# Create non-root user (UID/GID 65532) and ensure permissions for Docker socket
# - 'root' group (0): often owns /var/run/docker.sock
# - 'daemon' group (1): historically used for docker in some setups
RUN addgroup -g 65532 nonroot && \
    adduser -u 65532 -G nonroot -s /bin/false -D nonroot && \
    addgroup nonroot root && \
    addgroup nonroot daemon

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
