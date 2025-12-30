#!/bin/bash

# I.R.I.S Quick Start Script
echo "🤖 I.R.I.S Quick Start"
echo "======================"
echo ""

# Check if Ollama is running
echo "1️⃣  Checking Ollama..."
if pgrep -x "ollama" > /dev/null; then
    echo "   ✅ Ollama is running"
else
    echo "   ⚠️  Ollama not running, starting it..."
    ollama serve > /dev/null 2>&1 &
    sleep 5
    if pgrep -x "ollama" > /dev/null; then
        echo "   ✅ Ollama started successfully"
    else
        echo "   ❌ Failed to start Ollama"
        echo "   Try manually: ollama serve"
        exit 1
    fi
fi

# Check if CodeLlama model is available
echo ""
echo "2️⃣  Checking CodeLlama model..."
if ollama list | grep -q "codellama:13b"; then
    echo "   ✅ CodeLlama 13B is installed"
else
    echo "   ⚠️  CodeLlama 13B not found"
    echo "   Installing CodeLlama 13B..."
    ollama pull codellama:13b
fi

# Test Ollama connection
echo ""
echo "3️⃣  Testing Ollama connection..."
for i in 1 2 3 4 5; do
    if curl -s http://localhost:11434/api/tags > /dev/null 2>&1; then
        echo "   ✅ Ollama is responding"
        break
    else
        if [ "$i" -eq 5 ]; then
            echo "   ❌ Ollama is not responding"
            exit 1
        fi
        echo "   ⏳ Waiting for Ollama..."
        sleep 2
    fi
done

echo ""
echo "✅ All systems ready!"
echo ""
echo "🚀 Launching I.R.I.S GUI..."
echo ""

# Launch I.R.I.S GUI
mvn exec:java -Dexec.mainClass="com.jarvis.gui.JarvisGUI" -q
