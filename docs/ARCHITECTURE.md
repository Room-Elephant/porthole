# Architecture

Porthole is designed as a monolithic, single-artifact application for simplicity of deployment.

## Tech Stack

### Backend
- **Framework**: Spring Boot 4.0.0
- **Language**: Java 25
- **Concurrency**: Virtual threads enabled, parallel container mapping
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

## Container Status

Each container displays a status indicator (semaphore) in the top-right corner:

- **Green**: Container is running
- **Yellow**: Container is paused or restarting
- **Red**: Container is stopped, exited, or dead

Hovering over the indicator shows the full status (e.g., "Up 2 hours", "Exited (0) 3 days ago").

The UI provides toggles to:
- **Show stopped containers**: Include non-running containers (equivalent to `docker ps -a`)
- **Show containers without ports**: Include containers without exposed ports

## Version Detection

Porthole attempts to detect the current version of each container using multiple strategies (in priority order):

1. **Image-specific environment variable**: Looks for `<IMAGE_NAME>_VERSION` (e.g., `MONGO_VERSION`, `REDIS_VERSION`)
2. **Generic environment variable**: Falls back to `VERSION` if no image-specific var exists
3. **OCI labels**: Checks `org.opencontainers.image.version` or `version` labels
4. **Image tag**: Uses the tag from the image name (e.g., `7.0` from `mongo:7.0`)

The image-specific check (step 1) takes priority because containers often have multiple `*_VERSION` env vars (like `GOSU_VERSION`, `PYTHON_VERSION`) that aren't the application version.

## Docker Hub Integration

Porthole queries Docker Hub to detect available updates. When resolving image names:

- **Official images** (e.g., `redis`, `postgres`) are stored under the `library/` namespace
- **User/org images** (e.g., `bitnami/redis`) use their namespace directly

```
redis           → library/redis      (official)
bitnami/redis   → bitnami/redis      (third-party)
mongo:7         → library/mongo      (tag stripped for API calls)
```

This is required because the Docker Registry API expects the full path:
- ✅ `https://registry-1.docker.io/v2/library/redis/manifests/latest`
- ❌ `https://registry-1.docker.io/v2/redis/manifests/latest`

## Directory Structure

```
.
├── client/             # React Application
├── server/             # Spring Boot Application
│   └── src/main/java/com/roomelephant/porthole
├── Dockerfile          # Multi-stage build definition
└── docs/               # Documentation
```
