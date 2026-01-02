#!/bin/bash

# I.R.I.S Voice Assistant - Setup Script
# This script installs all required dependencies for the I.R.I.S voice assistant
# Supports: Ubuntu/Debian (apt), Arch Linux (pacman), Fedora (dnf)

echo "==========================================="
echo "I.R.I.S Voice Assistant - Setup"
echo "==========================================="
echo ""

# Check if running on Linux
if [[ "$OSTYPE" != "linux-gnu"* ]]; then
    echo "❌ This script is designed for Linux systems"
    exit 1
fi

# Detect package manager / distribution
detect_distro() {
    if command -v pacman &> /dev/null; then
        echo "arch"
    elif command -v apt-get &> /dev/null; then
        echo "debian"
    elif command -v dnf &> /dev/null; then
        echo "fedora"
    else
        echo "unknown"
    fi
}

DISTRO=$(detect_distro)
echo "📍 Detected distribution: $DISTRO"
echo ""

echo "📦 Installing system dependencies..."
echo ""

case $DISTRO in
    arch)
        echo "Using pacman (Arch Linux)..."
        sudo pacman -Syu --noconfirm
        sudo pacman -S --noconfirm jdk11-openjdk maven pulseaudio alsa-utils
        # Set Java 11 as default
        sudo archlinux-java set java-11-openjdk
        ;;
    debian)
        echo "Using apt-get (Ubuntu/Debian)..."
        sudo apt-get update
        sudo apt-get install -y openjdk-11-jdk maven pulseaudio alsa-utils
        ;;
    fedora)
        echo "Using dnf (Fedora)..."
        sudo dnf update -y
        sudo dnf install -y java-11-openjdk java-11-openjdk-devel maven pulseaudio alsa-utils
        ;;
    *)
        echo "❌ Unsupported distribution. Please install manually:"
        echo "   - Java 11 (OpenJDK)"
        echo "   - Maven"
        echo "   - PulseAudio and ALSA utilities"
        exit 1
        ;;
esac

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
