#!/usr/bin/env bash
# Saigai — build the debug APK (Git Bash / macOS / Linux variant).
#
# Why this file exists: if your default `java` is not JDK 17, Gradle 8.11.1 /
# AGP 8.7.3 fails with the unhelpful message "What went wrong: 25.0.1".
# This pins JDK 17 for the build only. Override with SAIGAI_JDK if yours lives
# somewhere else.
#
# Usage:  ./build-debug.sh          (debug APK)
#         ./build-debug.sh clean    (wipe build output first, then build)
set -euo pipefail

JDK="${SAIGAI_JDK:-C:/Users/preeb/Yazhi/tools/jdk17/jdk-17.0.21.1+2}"

if [ ! -x "$JDK/bin/java" ]; then
  echo "ERROR: JDK 17 not found at $JDK"
  echo "Install Temurin 17, or run: SAIGAI_JDK=/path/to/jdk17 ./build-debug.sh"
  exit 1
fi

export JAVA_HOME="$JDK"
cd "$(dirname "$0")"

if [ "${1:-}" = "clean" ]; then
  ./gradlew :app:clean
fi

./gradlew :app:assembleDebug

echo
echo "BUILD OK"
echo
echo "Phone (arm64) APK  : $(pwd)/app/build/outputs/apk/debug/app-arm64-v8a-debug.apk"
echo "Emulator (x86_64)  : $(pwd)/app/build/outputs/apk/debug/app-x86_64-debug.apk"
