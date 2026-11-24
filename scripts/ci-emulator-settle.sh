#!/usr/bin/env bash
# Prepare the CI emulator for tests. Also runs before AVD snapshot is saved.
#
# 1. Hide system error dialogs, so an ANR in System UI or launcher restarts
#    instead of showing an "isn't responding" dialog that takes window focus.
# 2. Wait until pending broadcasts are delivered.
# 3. Compile pending packages ahead of time.
# 4. Log guest CPU load until it looks idle.
# 5. Log the focused window and warn if an ANR dialog already holds focus.
#
# Usage: ci-emulator-settle.sh [--dexopt]
set -u

CPU_IDLE_PERCENT=25
CPU_IDLE_SAMPLES=3
CPU_SAMPLE_SECONDS=5
CPU_LOG_MAX_SECONDS=180

log() { echo "[settle +${SECONDS}s] $*"; }

timeout 60 adb shell settings put global hide_error_dialogs 1
value=$(timeout 60 adb shell settings get global hide_error_dialogs | tr -d '\r')
if [ "$value" != "1" ]; then
  echo "::error::hide_error_dialogs is '${value}', expected 1"
  exit 1
fi
log "hide_error_dialogs=1"

if timeout 300 adb shell am wait-for-broadcast-idle; then
  log "broadcast queues idle"
else
  echo "::warning::Broadcast queue not idle within 300 s; continuing anyway"
fi

if [ "${1:-}" = "--dexopt" ]; then
  start=$SECONDS
  if timeout 900 adb shell pm bg-dexopt-job; then
    log "pm bg-dexopt-job done in $((SECONDS - start)) s"
  else
    echo "::warning::pm bg-dexopt-job failed or timed out after $((SECONDS - start)) s; continuing"
  fi
fi

read_cpu() {
  timeout 30 adb shell cat /proc/stat 2>/dev/null | tr -d '\r' |
    awk '$1 == "cpu" { t = 0; for (i = 2; i <= 9; i++) t += $i; print t - $5 - $6, t; exit }'
}

prev=$(read_cpu)
if [ -z "$prev" ]; then
  echo "::warning::Could not read /proc/stat; skipping CPU log"
else
  idle_run=0
  result="cap reached (${CPU_LOG_MAX_SECONDS} s)"
  deadline=$((SECONDS + CPU_LOG_MAX_SECONDS))
  while [ "$SECONDS" -lt "$deadline" ]; do
    sleep "$CPU_SAMPLE_SECONDS"
    cur=$(read_cpu)
    if [ -z "$cur" ]; then
      result="stopped: could not read /proc/stat"
      break
    fi
    read -r b0 t0 <<< "$prev"
    read -r b1 t1 <<< "$cur"
    prev=$cur
    [ $((t1 - t0)) -gt 0 ] || continue
    busy=$((100 * (b1 - b0) / (t1 - t0)))
    log "guest cpu busy ${busy}%"
    if [ "$busy" -le "$CPU_IDLE_PERCENT" ]; then idle_run=$((idle_run + 1)); else idle_run=0; fi
    if [ "$idle_run" -ge "$CPU_IDLE_SAMPLES" ]; then
      result="looks idle"
      break
    fi
  done
  log "cpu log ${result}"
fi

focus=$(timeout 30 adb shell dumpsys window 2>/dev/null | tr -d '\r' | grep -E 'mCurrentFocus|mFocusedApp')
log "focus:"
echo "$focus"
if grep -q 'Application Not Responding' <<< "$focus"; then
  echo "::warning::A system 'isn't responding' dialog holds focus; UI tests will likely fail with RootViewWithoutFocusException"
fi

exit 0
