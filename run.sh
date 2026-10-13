#!/bin/bash
# Compiles and runs the app on macOS / Linux.
# Needs: JDK 17 or newer, and the MySQL Connector/J .jar inside the lib folder.
cd "$(dirname "$0")" || exit 1
mkdir -p out
javac -d out -cp "lib/*" $(find src -name "*.java") && java -cp "out:lib/*" ntm.Main
