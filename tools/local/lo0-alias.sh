#!/bin/bash
# RSProx addresses each world as 127.<id>>8>.<id&0xFF>.<2 + targetId>.
# World 1 + first custom target (id 1) => 127.0.1.3
# macOS only routes 127.0.0.1 by default, so this alias must exist for the
# client to reach the proxy. Requires sudo. Does not persist across reboots.
set -euo pipefail
IP="127.0.1.3"
MODE="${1:-add}"
if [ "$MODE" = "remove" ]; then
  sudo ifconfig lo0 -alias "$IP" && echo "removed alias $IP"
else
  sudo ifconfig lo0 alias "$IP" && echo "added alias $IP"
fi
