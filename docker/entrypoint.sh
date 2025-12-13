#!/bin/bash
set -e

SOCKET="/var/run/docker.sock"

# Check if the Docker socket exists
if [ -S "$SOCKET" ]; then
    # Get the GID of the socket
    GID=$(stat -c '%g' "$SOCKET")
    echo "Found docker socket with GID: $GID"

    # If the GID is not 0 (root), we need to ensure permissions
    if [ "$GID" != "0" ]; then
        # Check if a group with this GID already exists
        if ! getent group "$GID" >/dev/null; then
             echo "Creating group docker_sock with GID $GID"
             groupadd -g "$GID" docker_sock
        fi
        
        # Add nonroot user to the group
        GROUP=$(getent group "$GID" | cut -d: -f1)
        echo "Adding nonroot user to group $GROUP"
        usermod -aG "$GROUP" nonroot
    fi
else
    echo "Docker socket not found at $SOCKET"
fi

# Drop privileges and execute the command
exec gosu nonroot "$@"
