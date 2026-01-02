#!/bin/bash

# I.R.I.S GUI Launcher
# Simple script to launch the GUI version of I.R.I.S

echo "🤖 Starting I.R.I.S GUI..."
echo ""

# Change to script directory
cd "$(dirname "$0")"

# Check if already compiled
if [ ! -d "target/classes" ]; then
    echo "📦 First run - compiling project..."
    mvn compile -q
    if [ $? -ne 0 ]; then
        echo "❌ Compilation failed"
        exit 1
    fi
fi

# Launch GUI
mvn exec:java -Dexec.mainClass="com.jarvis.gui.JarvisGUI" -q
