#!/usr/bin/env sh
set -eu

if [ -z "${JAVA_HOME:-}" ] && [ -d "/Applications/Android Studio.app/Contents/jbr/Contents/Home" ]; then
  export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
fi

./gradlew assembleDebug

cp -f app/build/outputs/apk/debug/app-debug.apk "Sky Fury.apk"
echo "Copied debug APK to: $(pwd)/Sky Fury.apk"
