# Porthole ⚓️

> A modern, lightweight dashboard for your local Docker containers.

![Porthole Screenshot](docs/images/screenshot.png)

Porthole automatically discovers your running Docker containers and provides a beautiful, clean interface to access them. It resolves container icons automatically using [SimpleIcons](https://simpleicons.org/) and allows you to quickly jump to exposed ports.

## Features

- 🕵️‍♂️ **Auto-Discovery**: Automatically lists all running containers with exposed ports.
- 🎨 **Automated Icons**: Mentally maps container image names to icons (e.g., `redis` -> Redis Icon).
- 🌓 **Modern UI**: Sleek, dark-mode interface built with React and Vanilla CSS.
- 🔌 **Port Selection**: Remembers your preferred port for containers exposing multiple ports.
- 🐳 **Docker Native**: Runs as a single, lightweight Docker container.
- 🛠 **Customizable**: Override icon mappings via a simple JSON configuration.

## Quick Start

Run Porthole with a single Docker command:

```bash
docker run -d \
  -p 8080:8080 \
  -v /var/run/docker.sock:/var/run/docker.sock \
  --name porthole \
  porthole
```

**Note**: Mounting `/var/run/docker.sock` is required for Porthole to see your other containers.

Open [http://localhost:8080](http://localhost:8080) to view your dashboard.

## Configuration

Porthole attempts to match container image names to icons automatically. If you use custom image names or want to change an icon, you can provide a custom `icons.json` file.

See [Configuration Documentation](docs/CONFIGURATION.md) for details.

## Development

Porthole is a **Single JAR** application. The React frontend is built and bundled into the Spring Boot backend during the Maven build process.

### Prerequisites
- Java 21+
- Maven
- Node.js 20+ and npm (for frontend-only development)
- Docker (for containerized builds)

### Building from Source

#### Build Backend Only
Build just the Spring Boot backend without the frontend:
```bash
cd server
mvn clean package -DskipTests
```
The JAR will be in `server/target/porthole-0.0.1-SNAPSHOT.jar` but won't include frontend assets.

#### Build Frontend Only
Build just the React frontend:
```bash
cd frontend
npm install
npm run build
```
The built frontend will be in `frontend/dist/`.

#### Build Backend + Frontend (Complete Application)
Build the complete application with frontend bundled into the backend JAR:
```bash
cd server
mvn clean package -DskipTests -Pbuild-frontend
```
The frontend will be automatically built and copied into the JAR's static resources.

#### Build Docker Image
Build the containerized application:
```bash
# From the project root
docker build -t porthole:latest .
```
The Dockerfile uses a multi-stage build that automatically builds both frontend and backend.

### Running Locally

```bash
# Run the JAR directly
java -jar server/target/porthole-0.0.1-SNAPSHOT.jar

# Or run with Docker
docker run -p 9753:9753 -v /var/run/docker.sock:/var/run/docker.sock porthole:latest
```

Access the application at `http://localhost:9753`

See [Architecture Documentation](docs/ARCHITECTURE.md) for more details on the tech stack.

## License

MIT
