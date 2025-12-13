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
├── config/             # Template configuration files
├── dev/                # Development Docker files
│   ├── Dockerfile      # Multi-stage build for local development
│   └── compose.yml     # Development compose file
├── docs/               # Documentation
└── Dockerfile          # CI/production Dockerfile (uses pre-built JAR)
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

For CI/production, the root `Dockerfile` expects a pre-built native executable (built with `mvn -Pnative,copy-client native:compile`).

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

### IntelliJ IDEA

Pre-configured run configurations are available in `server/.run/`:

- **Porthole java local** - Runs the application with the `local` profile
- **Porthole container** - Remote debugging for containerized application (port 5005)

## Testing

### Server Tests

Run the Spring Boot backend tests:

```bash
cd server
mvn test
```

Generate a coverage report:

```bash
cd server
mvn verify
# Report available at server/target/site/jacoco/index.html
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

- **Server job**: Builds and tests the Spring Boot backend with JaCoCo coverage
- **Client job**: Installs, tests, and builds the React frontend with Vitest coverage
- **Docker job**: Builds GraalVM native image, pushes Docker image to `ghcr.io`, and creates GitHub Release

The Docker image is published to GitHub Container Registry at `ghcr.io/<owner>/porthole`.

See `.github/workflows/release.yml` for the full workflow definition.

