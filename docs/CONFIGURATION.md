# Configuration

Porthole uses a smart icon resolution strategy, but sometimes you need manual control. You can configure icon mappings using a JSON file.

## Icon Resolution Logic

1.  **Exact Match**: Checks `icons.json` (both internal defaults and your external overrides).
2.  **Simple Name**: Tries to fetch `https://cdn.simpleicons.org/<image-name>`.

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
The **Value** is the slug used by [SimpleIcons CDN](https://simpleicons.org/).

### 2. Run with Volume Mount

Mount your file to `/app/config/icons.json` inside the container.

```bash
docker run -d -p 8080:8080 \
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
