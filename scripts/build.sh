#!/bin/sh
# Packs datapacks/ender_pet into dist/ and prints a paste-able install script.
# Usage: scripts/build.sh   (then paste dist/install-ender_pet.sh into the server terminal)
set -e
cd "$(dirname "$0")/.."
mkdir -p dist
COPYFILE_DISABLE=1 tar --no-xattrs -czf dist/ender_pet.tgz -C datapacks ender_pet
SUM=$(shasum dist/ender_pet.tgz | cut -d' ' -f1)
{
  echo "cat > /tmp/ep.b64 <<'EOF'"
  base64 -i dist/ender_pet.tgz | fold -w 100
  echo "EOF"
  echo "base64 -di /tmp/ep.b64 > /tmp/ep.tgz && sha1sum /tmp/ep.tgz   # expect $SUM"
  echo "rm -rf /data/world/datapacks/ender_pet && tar xzf /tmp/ep.tgz -C /data/world/datapacks && rm /tmp/ep.b64 /tmp/ep.tgz && rcon-cli reload && rcon-cli datapack list"
} > dist/install-ender_pet.sh
echo "dist/install-ender_pet.sh written (sha1 $SUM)"
