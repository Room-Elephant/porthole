# Architecture

Porthole is designed as a monolithic, single-artifact application for simplicity of deployment.

## Tech Stack

### Backend
- **Framework**: Spring Boot 3.4.0
- **Language**: Java 21
- **Docker Client**: [docker-java](https://github.com/docker-java/docker-java) with `ZeroDepDockerHttpClient` (Unix Socket support).
- **Build Tool**: Maven

### Client
- **Framework**: React 18
- **Bundler**: Vite
- **Styling**: Vanilla CSS (Modern, Variables-based)
- **State**: Local React State + LocalStorage (for user preferences)

## Build Process

We use a "Client-First" build strategy integrated into Maven:

1.  **Client Build**: The `frontend-maven-plugin` runs `npm install` and `npm run build` in the `client/` directory.
2.  **Resource Copying**: The `maven-resources-plugin` copies the contents of `client/dist` into `server/target/classes/static`.
3.  **JAR Packaging**: Spring Boot packages everything into a single executable JAR.

This allows the final Docker image to just run `java -jar app.jar` without needing Node.js or a separate web server (Nginx) in the runtime container.

## Directory Structure

```
.
├── client/             # React Application
├── server/             # Spring Boot Application
│   └── src/main/java/com/roomelephant/porthole
├── Dockerfile          # Multi-stage build definition
└── docs/               # Documentation
```
