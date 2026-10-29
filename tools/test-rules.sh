#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
mkdir -p build/tests
javac -encoding UTF-8 -d build/tests app/src/main/java/com/example/archess/Chess.java app/src/main/java/com/example/archess/Game.java app/src/main/java/com/example/archess/BasicEngine.java tests/ChessTest.java
java -cp build/tests com.example.archess.ChessTest
