#!/usr/bin/env bash
# Wait until Android has delivered pending broadcasts (incl. BOOT_COMPLETED) after boot.
if ! timeout 300 adb shell am wait-for-broadcast-idle; then
  echo "::warning::Broadcast queue not idle within 300 s; continuing anyway"
fi
