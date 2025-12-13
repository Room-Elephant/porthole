#!/bin/bash
set -euo pipefail

IMAGE_NAME="${1:-porthole:ci-test}"
SOCKET_FILE="$(pwd)/test_socket.sock"
TEST_GID=1001

echo "Using image: $IMAGE_NAME"

cleanup() {
    rm -f "$SOCKET_FILE"
}
trap cleanup EXIT

# ---------------------------------------------------------
# Test 1: Default User (No Socket)
# ---------------------------------------------------------
echo ">> Test 1: Verifying default user (No socket mounted)..."
OUTPUT=$(docker run --rm --entrypoint /app/entrypoint.sh "$IMAGE_NAME" id)
echo "Output: $OUTPUT"

if echo "$OUTPUT" | grep -q "uid=65532"; then
    echo "SUCCESS: Correct default UID (nonroot)"
else
    echo "FAILURE: Incorrect UID. Expected 65532."
    exit 1
fi

# ---------------------------------------------------------
# Test 2: GID Handling (With Socket)
# ---------------------------------------------------------
echo ">> Test 2: Verifying GID handling (Socket mounted with GID $TEST_GID)..."

# Create dummy socket
python3 -c "import socket; s=socket.socket(socket.AF_UNIX); s.bind('$SOCKET_FILE')"

# Set permissions (requires sudo if running as non-root on Linux, which is typical for CI)
# We change owner to the container user (65532) and group to our test GID
if command -v sudo >/dev/null 2>&1; then
    sudo chown 65532:$TEST_GID "$SOCKET_FILE"
    sudo chmod 660 "$SOCKET_FILE"
else
    # Try without sudo (e.g. running as root in CI)
    chown 65532:$TEST_GID "$SOCKET_FILE"
    chmod 660 "$SOCKET_FILE"
fi

# Run container mounting the socket
OUTPUT=$(docker run --rm --entrypoint /app/entrypoint.sh -v "$SOCKET_FILE":/var/run/docker.sock "$IMAGE_NAME" id)
echo "Output: $OUTPUT"

if echo "$OUTPUT" | grep -q "$TEST_GID"; then
    echo "SUCCESS: User is correctly added to GID $TEST_GID"
else
    echo "FAILURE: User is NOT in GID $TEST_GID"
    exit 1
fi

echo "All tests passed!"
