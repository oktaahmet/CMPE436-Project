#!/bin/bash

echo "========================================"
echo "Word Game Server - Compile and Run"
echo "========================================"
echo

# Navigate to script directory
cd "$(dirname "$0")" || exit 1

echo "[1/3] Cleaning old build..."
if [ -d "bin" ]; then
    rm -rf bin
fi
mkdir bin

echo "[2/3] Compiling Java files..."
javac -d bin src/*.java

if [ $? -ne 0 ]; then
    echo
    echo "ERROR: Compilation failed!"
    read -p "Press Enter to exit..."
    exit 1
fi

echo "[3/3] Starting server..."
echo
java -cp bin com.example.wrd.GameServer

read -p "Press Enter to exit..."