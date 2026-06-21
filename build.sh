#!/bin/bash
./gradlew :app:assembleDebug --no-daemon --offline
if [ $? -eq 0 ]; then
  echo "¡Victoria! El bot se ha compilado correctamente."
else
  echo "Error: Revisa los archivos de configuración."
fi