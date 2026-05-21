#!/bin/zsh
# Compile + run WimbledonTennis3D outside IntelliJ.
# Usage:  ./run.sh    (from inside the WimbledonTennis3D folder)

set -e
PROJ="$(cd "$(dirname "$0")" && pwd)"
FX_LIB="$HOME/javafx-sdk-24/lib"

if [[ ! -d "$FX_LIB" ]]; then
  echo "JavaFX SDK not found at $FX_LIB"
  echo "Download from https://gluonhq.com/products/javafx/ and unzip to ~/javafx-sdk-24"
  exit 1
fi

OUT="$PROJ/out/production/WimbledonTennis3D"
mkdir -p "$OUT"

CP="$FX_LIB/javafx.base.jar:$FX_LIB/javafx.controls.jar:$FX_LIB/javafx.graphics.jar:$FX_LIB/javafx.media.jar"

echo "Compiling..."
javac -cp "$CP" -d "$OUT" "$PROJ/src/WimbledonTennis3D.java" "$PROJ/src/TennisGameApp.java"

echo "Running..."
java -classpath "$OUT:$CP" WimbledonTennis3D
