#!/usr/bin/env bash
# Record the README demo GIF and screenshot grid from a running emulator.
#
# What it produces:
#   docs/demo.gif                 ~40s flow: sign in -> save city -> weather -> Gemini insights -> map
#   docs/screenshots/0*.png       4-screenshot grid embedded in the README
#
# Prerequisites:
#   - An emulator is booted (Pixel-class image, API 29+), e.g.:
#       $ANDROID_SDK/emulator/emulator -avd Pixel_3a -no-snapshot-load
#     (note: -gpu host hung QEMU on macOS here; the default GPU mode works)
#   - local.properties has live GEMINI_API_KEY / OPENWEATHERMAP_API_KEY / MAPS_API_KEY
#     (the flow still works with blank keys, but the demo then shows fallback content)
#   - ffmpeg on PATH (brew install ffmpeg)
#
# Recording method: the EMULATOR CONSOLE recorder (`adb emu screenrecord`),
# which captures on the host. The device-side `adb shell screenrecord` fails
# with "Encoder failed (err=-38)" on this AVD at any resolution.
#
# Usage:
#   ./scripts/local/record_demo.sh
#
# The flow is driven BY HAND in the emulator window; this script handles
# install, capture, and GIF encoding. Keep the take under ~65s — it is
# encoded at 1.6x so the GIF lands in the 30-45s range. Suggested flow:
#   sign in -> ADD A LOCATION -> "Chicago" -> ADD -> WEATHER (wait for the
#   Gemini image) -> WEATHER INSIGHTS (tap a question, wait for the answer)
#   -> back, back -> MAP.

set -euo pipefail
cd "$(dirname "$0")/../.."

SDK="${ANDROID_HOME:-$HOME/Library/Android/sdk}"
ADB="$SDK/platform-tools/adb"
OUT_WEBM=/tmp/weather-planner-demo.webm

"$ADB" wait-for-device

echo "==> Building and installing debug APK"
./gradlew :app:installDebug --console=plain

echo "==> Enabling touch indicators and launching app"
"$ADB" shell settings put system show_touches 1
"$ADB" shell am start -n com.weatherplanner.app/.LoginActivity
sleep 2

echo "==> Capturing screenshots: press ENTER at each screen (city list, weather, map, insights); Ctrl-D to skip"
i=1
names=(01-cities 02-weather 03-map 04-insights)
for name in "${names[@]}"; do
    read -r -p "  [$i/4] capture docs/screenshots/$name.png? " _ || break
    "$ADB" exec-out screencap -p > "docs/screenshots/$name.png"
    echo "      saved docs/screenshots/$name.png"
    i=$((i + 1))
done

read -r -p "==> Position the app at the login screen, then press ENTER to START recording (Ctrl-C to abort) " _
"$ADB" emu screenrecord start --time-limit 90 "$OUT_WEBM"
echo "==> RECORDING - drive the flow now"
read -r -p "==> Press ENTER to STOP recording " _
"$ADB" emu screenrecord stop
sleep 3

echo "==> Encoding docs/demo.gif (1.6x speed, 10fps, 320px wide)"
ffmpeg -y -v error -i "$OUT_WEBM" \
    -vf "setpts=PTS/1.6,fps=10,scale=320:-1:flags=lanczos,palettegen=max_colors=128" /tmp/demo-palette.png
ffmpeg -y -v error -i "$OUT_WEBM" -i /tmp/demo-palette.png \
    -filter_complex "setpts=PTS/1.6,fps=10,scale=320:-1:flags=lanczos[x];[x][1:v]paletteuse=dither=bayer:bayer_scale=5" \
    docs/demo.gif

ls -lh docs/demo.gif docs/screenshots/
echo "==> Done. Review the GIF frame-by-frame for anything sensitive before committing."
