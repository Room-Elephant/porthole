# Configuration

## Configuration Files

The Docker image includes template configuration files in `/app/config/`:

| File | Purpose |
|------|---------|
| `application.yml` | Main application settings (Docker host, registry timeouts, icon CDN) |
| `icons.yml` | Custom icon mappings |

You can mount your own files to override these templates:

```bash
docker run -d -p 9753:9753 \
  -v /var/run/docker.sock:/var/run/docker.sock \
  -v $(pwd)/my-config:/app/config \
  --name porthole porthole
```

## API Endpoints

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/api/containers` | GET | Returns all containers. Supports `includeWithoutPorts` and `includeStopped` query params |
| `/api/containers/{containerId}/version` | GET | Returns version info for a container (current version, latest version, update availability) |

## Health Check

Porthole exposes a health endpoint via Spring Actuator:

```
GET /actuator/health
```

The health endpoint includes a Docker connectivity check that verifies the Docker daemon is reachable. If the Docker socket is unavailable or unresponsive, the health status will report as DOWN.

The Docker container includes a built-in HEALTHCHECK that polls this endpoint every 30 seconds.

## Response Compression

Porthole automatically compresses JSON responses larger than 1KB using gzip, reducing bandwidth usage for the container list API.

## Graceful Shutdown

When stopping Porthole, active requests are allowed up to 20 seconds to complete before the application terminates. This ensures in-flight API calls are not abruptly dropped.

## Docker Host

By default, Porthole connects to Docker via `unix:///var/run/docker.sock`. You can override this using an environment variable:

```bash
docker run -d -p 9753:9753 \
  -e DOCKER_HOST=tcp://localhost:2375 \
  --name porthole porthole
```

## Registry Configuration

Porthole queries Docker Hub to check for updates. You can configure timeouts and cache settings:

| Property | Default | Description |
|----------|---------|-------------|
| `REGISTRY_TIMEOUT_CONNECT` | `5s` | Connection timeout for Docker Hub API |
| `REGISTRY_TIMEOUT_READ` | `10s` | Read timeout for Docker Hub API |
| `REGISTRY_CACHE_TTL` | `1h` | How long to cache version information |
| `REGISTRY_CACHE_VERSION_MAX_SIZE` | `100` | Maximum cached version entries |

## Icon Mappings

Porthole uses a smart icon resolution strategy, but sometimes you need manual control. You can configure icon mappings using a YAML file.

## Icon CDN

By default, icons are fetched from Dashboard Icons CDN. You can customize the source:

| Property | Default | Description |
|----------|---------|-------------|
| `DASHBOARD_ICONS_URL` | `https://cdn.jsdelivr.net/gh/homarr-labs/dashboard-icons/webp` | Base URL for icons |
| `DASHBOARD_ICONS_EXTENSION` | `.webp` | File extension |

## Icon Resolution Logic

1.  **Exact Match**: Checks `icons.yml` (both internal defaults and your external overrides).
2.  **Dashboard Icons**: Fetches from the configured CDN using the image name.

## Customizing Icons

You can override defaults or add new mappings by editing `/app/config/icons.yml` or mounting your own file.

### 1. Create YAML File

```yaml
# Maps container image names to Dashboard Icons slugs
my-custom-redis: redis
postgres: postgresql
custom-app: react
```

The **Key** is the container image name (or simple name like `postgres`).
The **Value** is the icon slug from [Dashboard Icons](https://github.com/homarr-labs/dashboard-icons).

### 2. Run with Volume Mount

The Docker image includes a template at `/app/config/icons.yml`. Mount your file to override it:

```bash
docker run -d -p 9753:9753 \
  -v /var/run/docker.sock:/var/run/docker.sock \
  -v $(pwd)/my-icons.yml:/app/config/icons.yml \
  --name porthole porthole
```

## Default Mappings

The application comes with these built-in defaults:

| Image Name | Mapped Icon Slug |
| :--- | :--- |
| `mongo` | `mongodb` |
| `postgres` | `postgresql` |
| `cassandra` | `apache-cassandra` |

Overriding these keys in your external file will take precedence.

## Per-Container Settings

Each container has a settings panel accessible via the gear icon. Settings are stored in your browser's local storage.

### Port Selection

For containers with multiple exposed ports, you can select which port to use when clicking the container tile. The selected port is remembered across sessions.

### Version Update Checking

You can enable or disable version update checking for individual containers. When disabled:
- No requests are made to check for newer versions
- The update warning indicator (⚠️) is not shown

This is useful for:
- Local development images that don't need update checks
- Containers with private registries that Porthole cannot query
- Reducing network requests for containers you don't want to update
