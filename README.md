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
- Docker

### Building from Source

```bash
# Clean and Build (Frontend + Backend)
cd server
mvn clean package

# Run Locally
java -jar target/porthole-0.0.1-SNAPSHOT.jar
```

See [Architecture Documentation](docs/ARCHITECTURE.md) for more details on the tech stack.

## License

MIT
