#!/bin/sh
# Compila e abre o sistema (Linux/macOS). Requer Java 17 ou superior.
cd "$(dirname "$0")" || exit 1
mkdir -p out
javac -encoding UTF-8 -sourcepath src -d out src/app/Main.java || exit 1
java -cp "out:lib/*" app.Main
