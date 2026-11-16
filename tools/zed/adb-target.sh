#!/usr/bin/env bash
# Usage: tools/zed/adb-target.sh <gradle args...>
# Picks the Control Hub over USB if present, else over Wi-Fi, then runs ./gradlew with ANDROID_SERIAL set.
set -u

HUB_ADDR="${HUB_ADDR:-192.168.43.1:5555}"
USB_WAIT_SECONDS="${USB_WAIT_SECONDS:-3}"
WIFI_READY_SECONDS="${WIFI_READY_SECONDS:-10}"

log() { echo "[adb-target] $*"; }

usb_serial() {
  adb devices | awk '$2=="device" && $1!~/:/ {print $1; exit}'
}

log "config: hub=$HUB_ADDR usb_wait=${USB_WAIT_SECONDS}s wifi_ready=${WIFI_READY_SECONDS}s gradle_args=$*"
adb start-server

serial=""
for ((i = 0; i < USB_WAIT_SECONDS; i++)); do
  serial="$(usb_serial)"
  [ -n "$serial" ] && break
  sleep 1
done

if [ -n "$serial" ]; then
  log "using USB device $serial"
else
  log "no USB device after ${USB_WAIT_SECONDS}s, trying Wi-Fi $HUB_ADDR"
  if ! nc -z -G 3 "${HUB_ADDR%:*}" "${HUB_ADDR#*:}"; then
    log "ERROR: $HUB_ADDR unreachable in 3s. Plug in USB or join the hub Wi-Fi"
    exit 1
  fi
  adb disconnect "$HUB_ADDR" >/dev/null 2>&1
  adb connect "$HUB_ADDR"
  for ((i = 0; i < WIFI_READY_SECONDS; i++)); do
    [ "$(adb -s "$HUB_ADDR" get-state 2>/dev/null)" = "device" ] && serial="$HUB_ADDR" && break
    sleep 1
  done
  if [ -z "$serial" ]; then
    log "ERROR: $HUB_ADDR not in 'device' state after ${WIFI_READY_SECONDS}s (state: $(adb -s "$HUB_ADDR" get-state 2>&1)). Disconnecting ghost entry"
    adb disconnect "$HUB_ADDR" >/dev/null 2>&1
    exit 1
  fi
  log "using Wi-Fi device $serial"
fi

export ANDROID_SERIAL="$serial"
exec ./gradlew "$@"
