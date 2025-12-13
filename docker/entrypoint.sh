#!/bin/bash
set -euo pipefail

SOCKET="/var/run/docker.sock"
USER="nonroot"

if [ -S "$SOCKET" ]; then
    GID=$(stat -c '%g' "$SOCKET")
    echo "Docker socket detected with GID=$GID"

    if [ "$GID" -eq 0 ]; then
        echo "Docker Desktop detected; staying root"
        exec "$@"
    fi

    if ! getent group "$GID" >/dev/null; then
        GROUP_NAME="dockersock-$GID"
        echo "Creating group $GROUP_NAME with GID $GID"
        groupadd -g "$GID" "$GROUP_NAME"
    fi

    GROUP_NAME=$(getent group "$GID" | cut -d: -f1)

    if ! id -nG "$USER" | tr ' ' '\n' | grep -qx "$GROUP_NAME"; then
        echo "Adding $USER to group $GROUP_NAME"
        usermod -aG "$GROUP_NAME" "$USER"
    fi

    if ! gosu "$USER" test -r "$SOCKET" -a -w "$SOCKET"; then
        echo "Warning: $USER may not have read/write access to $SOCKET"
    fi
else
    echo "Docker socket not found at $SOCKET"
fi

exec gosu "$USER" "$@"
