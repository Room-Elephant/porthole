# Development Guide

This guide covers everything you need to build, test, and run Porthole locally.

For deeper technical details on how Porthole works, see the [Architecture](ARCHITECTURE.md) documentation.

## Prerequisites

- Java 25+ (GraalVM required for native builds)
- Maven
- Node.js 24+ and npm
- Docker

## Project Structure

```
.
├── client/             # React frontend (Vite + React 19)
├── server/             # Spring Boot backend (Java 25)
├── docker/             # Docker configuration
│   ├── templates/      # Template configuration files
│   ├── Dockerfile       # CI/production Dockerfile (uses pre-built native executable)
│   └── entrypoint.sh   # Entrypoint script
├── dev/                # Development Docker files
│   ├── Dockerfile       # Multi-stage build for local development
│   └── compose.yml     # Development compose file
└── docs/               # Documentation
```

Porthole is built as a **GraalVM native image**. The React client is bundled into the Spring Boot backend, which is then compiled to a native executable for fast startup and low memory usage.

## Building from Source

### Server Only

Build just the Spring Boot backend without the client:

```bash
cd server
mvn clean package -DskipTests
```

The JAR will be in `server/target/porthole-0.0.1-SNAPSHOT.jar` but won't include client assets.

### Client Only

Build just the React client:

```bash
cd client
npm install
npm run build
```

The built client will be in `client/dist/`.

### Full Application (JAR)

Build the complete application with client bundled into the backend JAR:

```bash
cd server
mvn clean package -DskipTests -Pbuild-client
```

The client will be automatically built and copied into the JAR's static resources.

If you've already built the client separately, you can skip the node/npm steps and just copy the dist folder:

```bash
cd server
mvn clean package -DskipTests -Pcopy-client
```

### Native Image

Build a GraalVM native executable for faster startup and lower memory usage:

```bash
# Build client first
cd client && npm run build && cd ..

# Build native image with client bundled
cd server
mvn -Pnative,copy-client native:compile -DskipTests
```

The native executable will be in `server/target/porthole`. First compilation takes 3-5 minutes; subsequent builds are faster with caching.

**Requirements**: GraalVM JDK 25+ must be installed and configured as your JAVA_HOME.

### Docker Image

For local development, use the multi-stage Dockerfile in `dev/`:

```bash
# From the project root
docker build -f dev/Dockerfile -t porthole:latest .

# Or use docker compose
docker compose -f dev/compose.yml up --build
```

The development Dockerfile uses a multi-stage build that automatically builds both client and server. Dependencies are cached for faster rebuild times.

For CI/production, the `docker/Dockerfile` expects a pre-built native executable (built with `mvn -Pnative,copy-client native:compile`).

## Running Locally

### From Native Executable

```bash
./server/target/porthole --spring.profiles.active=local
```

### From JAR

```bash
java -jar server/target/porthole-0.0.1-SNAPSHOT.jar --spring.profiles.active=local
```

### With Docker

```bash
docker run -p 9753:9753 -v /var/run/docker.sock:/var/run/docker.sock porthole:latest
```

Access the application at [http://localhost:9753](http://localhost:9753)

## Testing

### Server Tests

Run the Spring Boot backend tests:

```bash
cd server
mvn test   # Single run
mvn verify # Report available at server/target/site/jacoco/index.html
```

### Client Tests

Run the React client unit tests:

```bash
cd client
npm test              # Watch mode
npm run test:run      # Single run
npm run test:coverage # With coverage report
```

## Development Workflow

For active development, you can run the client and server separately:

1. **Start the backend** (from `server/`):
   ```bash
   mvn spring-boot:run
   ```

Or use IntelliJ IDEA Pre-configured run configurations available in `server/.run/`:

- **Porthole java local** - Runs the application with the `local` profile
- **Porthole container** - Remote debugging for containerized application (port 5005)

2. **Start the client dev server** (from `client/`):
   ```bash
   npm run dev
   ```

The client dev server proxies API requests to the backend.

## GitHub Actions Workflows

### CI Workflow

Runs on push/PR to `main`. Detects which parts of the codebase changed and only runs relevant jobs:

- **Server job**: Builds and tests the Spring Boot backend (skipped if no `server/` changes)
- **Client job**: Builds and tests the React frontend (skipped if no `client/` changes)

See `.github/workflows/ci.yml` for the full workflow definition.

### Release Workflow

Runs when a version tag (e.g., `v1.0.0`) is pushed:

- **Client job**: Installs, tests, and builds the React frontend, saving build artifacts.
- **Server Test job**: Runs backend tests with GraalVM.
- **Build & Push job**: Runs a matrix build for `amd64` and `arm64` that:
  - Builds the GraalVM native executable for the specific architecture.
  - Pushes architecture-specific Docker images.
- **Manifest job**:
  - Creates a multi-arch Docker manifest combining the images.
  - Pushes the final version tag (and `latest` for stable releases).
  - Creates the GitHub Release with auto-generated notes.

The Docker image is published to GitHub Container Registry at `ghcr.io/room-elephant/porthole`.

See `.github/workflows/release.yml` for the full workflow definition.

## Docker Production Setup

### Base Image Strategy
Given a standard GraalVM native-image build (glibc-based, dynamically linked), **`debian:bookworm-slim`** is the correct runtime choice instead of Alpine.
- **Reason**: The native binary built by GraalVM is dynamically linked against `glibc` (present in the build stage).
- **Compatibility**: Alpine uses `musl`, which makes it incompatible with standard glibc-linked binaries without complex static linking configurations.
- **Trade-off**: Bookworm-slim offers a stable, multi-arch foundation and is relatively small (~75MB), providing a good balance between size and compatibility.

### Permissions & Security
To run securely as a non-root user while accessing the Docker socket:

1. **Build Time**:
   - Creates a `nonroot` user with UID/GID 65532.
   - Adds this user to `root` (GID 0) and `daemon` (GID 1) groups, as these historically or commonly own `/var/run/docker.sock`.

2. **Run Time (Entrypoint)**:
   - The container starts as `root` (via `entrypoint.sh`) to perform setup.
   - **Socket Detection**: Checks the GID of the mounted `/var/run/docker.sock`.
   - **Dynamic Permissions**:
     - If the socket is owned by a GID that doesn't exist (e.g., 998), it dynamically creates a group for it.
     - Adds the `nonroot` user to this new group.
   - **Privilege Drop**: Finally, uses `gosu` to switch to the `nonroot` user before executing the main application.

This ensures the application process itself runs without root privileges but still has the exact access needed to talk to the host's Docker daemon.

