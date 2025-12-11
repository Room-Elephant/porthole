# Development Guide

This guide covers everything you need to build, test, and run Porthole locally.

For deeper technical details on how Porthole works, see the [Architecture](ARCHITECTURE.md) documentation.

## Prerequisites

- Java 25+
- Maven
- Node.js 24+ and npm
- Docker

## Project Structure

```
.
├── client/             # React frontend (Vite + React 19)
├── server/             # Spring Boot backend (Java 25)
├── config/             # Template configuration files
├── docs/               # Documentation
├── Dockerfile          # Multi-stage build definition
└── compose.yml         # Development compose file
```

Porthole is a **Single JAR** application. The React client is built and bundled into the Spring Boot backend during the Maven build process.

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

### Full Application

Build the complete application with client bundled into the backend JAR:

```bash
cd server
mvn clean package -DskipTests -Pbuild-client
```

The client will be automatically built and copied into the JAR's static resources.

### Docker Image

Build the containerized application:

```bash
# From the project root
docker build -t porthole:latest .
```

The Dockerfile uses a multi-stage build that automatically builds both client and server. Dependencies are cached for faster rebuild times.

## Running Locally

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

