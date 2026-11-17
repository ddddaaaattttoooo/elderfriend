#!/bin/sh
# Install Elderfriend and make it the home screen over USB.
# Usage: tools/setup.sh [path/to/Elderfriend.apk]
# Nothing is uninstalled; only Elderfriend is installed and granted permissions.
set -eu

PKG=ge.elderfriend.launcher
ACT=$PKG/.MainActivity

say() { printf '%s\n' "$*"; }
die() { printf 'error: %s\n' "$*" >&2; exit 1; }

command -v adb >/dev/null 2>&1 || die "adb not found. Install Android platform-tools."

DEVICES=$(adb devices | awk 'NR>1 && $2=="device" {print $1}')
[ -n "$DEVICES" ] || die "no authorized phone found. Enable USB debugging and accept the prompt."
COUNT=$(printf '%s\n' "$DEVICES" | wc -l | tr -d ' ')
[ "$COUNT" -eq 1 ] || die "more than one device connected: $DEVICES (use ADB_SERIAL)"
[ -n "${ADB_SERIAL:-}" ] && export ANDROID_SERIAL=$ADB_SERIAL

APK=${1:-}
if [ -z "$APK" ]; then
  APK=$(ls -t "$HOME"/Downloads/Elderfriend-*.apk 2>/dev/null | head -1 || true)
fi
[ -n "$APK" ] && [ -f "$APK" ] || die "APK not found. Pass its path: tools/setup.sh Elderfriend-1.0.0.apk"

say "Installing $APK"
adb install -r "$APK"

say "Granting permissions"
for p in READ_CONTACTS READ_CALL_LOG CALL_PHONE READ_PHONE_STATE; do
  adb shell pm grant "$PKG" "android.permission.$p" 2>/dev/null || say "  (already granted: $p)"
done

say "Setting Elderfriend as the home app"
if ! adb shell cmd package set-home-activity "$ACT" 2>/dev/null; then
  say "  Could not set it automatically. On the phone open"
  say "  Settings -> Apps -> Default apps -> Home app -> Elderfriend."
fi

say "Opening Elderfriend"
adb shell am start -n "$ACT" >/dev/null 2>&1 || true

say ""
say "Done. On the phone, finish the first-run setup screen."
say "Optional extras are described in PROMPT.md: bigger text, longer timeout, debloat."
