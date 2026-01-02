# I.R.I.S - Intelligent Responsive Integrated System

A sophisticated AI-powered voice assistant for Linux, built with Java. This assistant can understand voice commands, respond with natural speech, control your system, search the web, and have intelligent conversations.

## 🌟 Features

- **🎤 Voice Recognition**: Continuous listening with wake word detection ("Jarvis" or "Hey Jarvis")
- **🗣️ Text-to-Speech**: Natural voice responses using FreeTTS
- **🤖 AI Conversations**: Powered by Google Gemini API OR local LLM (Ollama) for offline use
- **💻 System Control**: Open applications, control volume, manage power
- **🌐 Web Integration**: Google search, YouTube, Wikipedia
- **🌤️ Weather Updates**: Real-time weather information
- **⏰ Time & Date**: Quick time and date queries
- **📝 Text Mode**: Fallback text input mode when voice recognition is unavailable
- **🔒 Offline Mode**: Run completely offline with local LLM (no API keys needed!)
- **🖥️ Modern GUI**: Sleek graphical interface with dark theme

## 🚀 Quick Start

### Option 1: GUI Mode (Recommended)
```bash
./start-jarvis-gui.sh
```

### Option 2: Terminal Mode
```bash
./start.sh
```

### Option 3: Quick Start Script
```bash
./quick-start.sh
```

## 📋 Prerequisites

- **Java 11 or higher**
- **Maven 3.6+**
- **Linux OS** (tested on Arch Linux, Ubuntu/Debian)
- **Microphone** (for voice input)
- **Ollama** (for offline AI - optional)

## 🔧 Installation Guide

### 1. Clone the Project

```bash
git clone https://github.com/saranrit2023/Intelegence_Responsive_Integrate_System.git
cd Intelegence_Responsive_Integrate_System
```

### 2. Install System Dependencies

**For Arch Linux:**
```bash
sudo pacman -S jdk11-openjdk maven pulseaudio
# For Ollama (optional - for offline AI)
yay -S ollama
```

**For Ubuntu/Debian:**
```bash
sudo apt-get update
sudo apt-get install -y openjdk-11-jdk maven pulseaudio
# For Ollama (optional)
curl -fsSL https://ollama.com/install.sh | sh
```

### 3. Configure Environment (Optional - for online features)

```bash
cp .env.example .env
```

Edit `.env` and add your API keys:
```bash
GEMINI_API_KEY=your_gemini_api_key
OPENWEATHER_API_KEY=your_openweather_api_key
```

### 4. Build the Project

```bash
mvn clean install -DskipTests
```

### 5. Run I.R.I.S

```bash
./start-jarvis-gui.sh
```

## 🎯 Command Examples

### System Commands
- "Open Firefox"
- "Open terminal"
- "Volume up / Volume down"
- "Mute volume"

### Web Commands
- "Search Google for Java tutorials"
- "Play music on YouTube"
- "Wikipedia artificial intelligence"

### Information Queries
- "What time is it?"
- "What's the date?"
- "What's the weather?"

### AI Conversations
- "Tell me a joke"
- "What is quantum computing?"
- "Explain machine learning"

### Exit
- "Exit" / "Quit" / "Goodbye"

## 📁 Project Structure

```
Intelegence_Responsive_Integrate_System/
├── pom.xml                     # Maven configuration
├── .env.example                # Environment template
├── README.md                   # This file
├── DOCUMENTATION.md            # Detailed documentation
├── start.sh                    # Terminal mode launcher
├── start-jarvis-gui.sh         # GUI mode launcher
├── quick-start.sh              # Quick setup script
├── setup.sh                    # Full setup script
└── src/
    ├── main/java/com/jarvis/
    │   ├── JarvisAssistant.java    # Main entry point
    │   ├── gui/
    │   │   └── JarvisGUI.java      # Graphical interface
    │   ├── config/
    │   │   └── Config.java         # Configuration
    │   ├── speech/
    │   │   ├── SpeechRecognizer.java
    │   │   └── TextToSpeech.java
    │   ├── commands/
    │   │   ├── CommandHandler.java
    │   │   ├── SystemCommands.java
    │   │   └── WebCommands.java
    │   ├── ai/
    │   │   └── AIProcessor.java    # AI integration
    │   ├── utils/
    │   │   ├── NetworkChecker.java
    │   │   └── FuzzyMatcher.java
    │   └── services/
    │       └── WeatherService.java
    └── test/                       # Unit tests
```

## 🐛 Troubleshooting

### Voice Recognition Not Working
1. Check microphone permissions
2. Test: `arecord -d 5 test.wav && aplay test.wav`
3. Use text mode as fallback

### No Audio Output
```bash
pulseaudio --check
pulseaudio --start
```

### Build Errors
```bash
mvn clean install -U
java -version  # Should be 11+
```

## 🔒 Security Notes

- Never commit your `.env` file to version control
- Keep API keys secure and rotate them regularly
- The `.gitignore` is configured to exclude sensitive files

## 📝 License

This project is provided for educational and personal use.

## 🤝 Contributing

Feel free to fork, modify, and enhance this project!

---

**Developed by Saran | Powered by AI 🚀**
