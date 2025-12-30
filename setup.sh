#!/bin/bash

# JARVIS Voice Assistant - Setup Script
# This script installs all required dependencies for the JARVIS voice assistant

echo "=========================================="
echo "JARVIS Voice Assistant - Setup"
echo "=========================================="
echo ""

# Check if running on Linux
if [[ "$OSTYPE" != "linux-gnu"* ]]; then
    echo "❌ This script is designed for Linux systems"
    exit 1
fi

echo "📦 Installing system dependencies..."
echo ""

# Update package list
echo "Updating package list..."
sudo apt-get update

# Install Java 11
echo "Installing Java 11..."
sudo apt-get install -y openjdk-11-jdk

# Install Maven
echo "Installing Maven..."
sudo apt-get install -y maven

# Install audio dependencies
echo "Installing audio dependencies..."
sudo apt-get install -y pulseaudio alsa-utils

# Verify installations
echo ""
echo "=========================================="
echo "Verifying installations..."
echo "=========================================="
echo ""

# Check Java
if command -v java &> /dev/null; then
    echo "✅ Java installed:"
    java -version
else
    echo "❌ Java installation failed"
    exit 1
fi

echo ""

# Check Maven
if command -v mvn &> /dev/null; then
    echo "✅ Maven installed:"
    mvn -version
else
    echo "❌ Maven installation failed"
    exit 1
fi

echo ""
echo "=========================================="
echo "Building the project..."
echo "=========================================="
echo ""

# Build the project
mvn clean install

if [ $? -eq 0 ]; then
    echo ""
    echo "=========================================="
    echo "✅ Setup completed successfully!"
    echo "=========================================="
    echo ""
    echo "📋 Next steps:"
    echo ""
    echo "1. Get your API keys:"
    echo "   📌 Gemini API: https://makersuite.google.com/app/apikey"
    echo "   📌 Google Cloud Speech: https://console.cloud.google.com/"
    echo "   📌 OpenWeather (optional): https://openweathermap.org/api"
    echo ""
    echo "2. Create .env file:"
    echo "   cp .env.example .env"
    echo ""
    echo "3. Edit .env and add your API keys:"
    echo "   nano .env"
    echo ""
    echo "4. Run JARVIS:"
    echo "   mvn exec:java -Dexec.mainClass=\"com.jarvis.JarvisAssistant\""
    echo ""
    echo "   Or use the JAR file:"
    echo "   java -jar target/jarvis-voice-assistant-1.0.0.jar"
    echo ""
    echo "📖 For more help, see:"
    echo "   - README.md (full documentation)"
    echo "   - QUICKSTART.md (quick reference)"
    echo ""
else
    echo ""
    echo "❌ Build failed. Please check the errors above."
    exit 1
fi
