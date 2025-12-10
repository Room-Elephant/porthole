# Configuration

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

The Docker container includes a built-in HEALTHCHECK that polls this endpoint every 30 seconds.

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

## Icon Mappings

Porthole uses a smart icon resolution strategy, but sometimes you need manual control. You can configure icon mappings using a JSON file.

## Icon CDN

By default, icons are fetched from Dashboard Icons CDN. You can customize the source:

| Property | Default | Description |
|----------|---------|-------------|
| `DASHBOARD_ICONS_URL` | `https://cdn.jsdelivr.net/gh/homarr-labs/dashboard-icons/webp` | Base URL for icons |
| `DASHBOARD_ICONS_EXTENSION` | `.webp` | File extension |

## Icon Resolution Logic

1.  **Exact Match**: Checks `icons.json` (both internal defaults and your external overrides).
2.  **Dashboard Icons**: Fetches from the configured CDN using the image name.

## Customizing Icons

You can override defaults or add new mappings by creating a JSON file (e.g., `my-icons.json`) and mounting it to the container.

### 1. Create JSON File

```json
{
  "my-custom-redis": "redis",
  "postgres": "postgresql",
  "custom-app": "react"
}
```

The **Key** is the container image name (or simple name like `postgres`).
The **Value** is the icon slug from [Dashboard Icons](https://github.com/homarr-labs/dashboard-icons).

### 2. Run with Volume Mount

Mount your file to `/app/config/icons.json` inside the container.

```bash
docker run -d -p 9753:9753 \
  -v /var/run/docker.sock:/var/run/docker.sock \
  -v $(pwd)/my-icons.json:/app/config/icons.json \
  --name porthole porthole
```

## Default Mappings

The application comes with these built-in defaults:

| Image Name | Mapped Icon Slug |
| :--- | :--- |
| `mongo` | `mongodb` |
| `postgres` | `postgresql` |

Overriding these keys in your external file will take precedence.
