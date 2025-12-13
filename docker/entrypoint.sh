#!/bin/bash
set -e

SOCKET="/var/run/docker.sock"

if [ -S "$SOCKET" ]; then
    GID=$(stat -c '%g' "$SOCKET")
    echo "Found docker socket with GID: $GID"

    if [ "$GID" != "0" ]; then
        if ! getent group "$GID" >/dev/null; then
             echo "Creating group docker_sock with GID $GID"
             groupadd -g "$GID" docker_sock
        fi
        
        GROUP=$(getent group "$GID" | cut -d: -f1)
        echo "Adding nonroot user to group $GROUP"
        usermod -aG "$GROUP" nonroot
    fi
else
    echo "Docker socket not found at $SOCKET"
fi

exec gosu nonroot "$@"
