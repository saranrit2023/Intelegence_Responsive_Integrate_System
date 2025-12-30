# I.R.I.S - Intelligent Responsive Integrated System

## Complete Project Documentation

---

## Table of Contents

1. [Project Overview](#1-project-overview)
2. [Architecture](#2-architecture)
3. [Installation & Setup](#3-installation--setup)
4. [Running the Project](#4-running-the-project)
5. [Source Code Breakdown](#5-source-code-breakdown)
6. [Configuration Guide](#6-configuration-guide)
7. [Features & Commands](#7-features--commands)
8. [Troubleshooting](#8-troubleshooting)

---

## 1. Project Overview

**I.R.I.S** (Intelligent Responsive Integrated System) is an AI-powered voice assistant designed for Linux, similar to Iron Man's JARVIS. It combines:

- **Voice Recognition**: Offline (Vosk) or Online (Google Cloud Speech-to-Text)
- **Text-to-Speech**: FreeTTS engine for voice responses
- **AI Processing**: Multiple AI backends - Ollama (offline), Gemini, and Grok (online)
- **System Automation**: Open applications, control volume, execute commands
- **Security Tools**: File analysis, link checking, penetration testing tools
- **Modern GUI**: Green-themed interface with animated background

### Technology Stack

| Component | Technology |
|-----------|------------|
| Language | Java 11+ |
| Build Tool | Maven |
| Speech Recognition | Vosk (offline) / Google Cloud (online) |
| Text-to-Speech | FreeTTS |
| AI Processing | Ollama, Google Gemini, Grok |
| HTTP Client | OkHttp |
| JSON Processing | Gson |
| Configuration | Dotenv |
| GUI | Java Swing |

---

## 2. Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                        I.R.I.S System                           │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  ┌─────────────┐    ┌──────────────┐    ┌─────────────────┐    │
│  │  JarvisGUI  │───▶│CommandHandler│───▶│   AIProcessor   │    │
│  │  (Swing UI) │    │              │    │(Gemini/Grok/    │    │
│  └──────┬──────┘    └──────┬───────┘    │    Ollama)      │    │
│         │                  │            └─────────────────┘    │
│         │                  │                                    │
│  ┌──────▼──────┐    ┌──────▼───────┐    ┌─────────────────┐    │
│  │ Speech-     │    │SystemCommands│    │ SecurityTools   │    │
│  │ Recognizer  │    │ WebCommands  │    │ FileAnalyzer    │    │
│  │ TextToSpeech│    │AppAutomation │    │ LinkChecker     │    │
│  └─────────────┘    └──────────────┘    └─────────────────┘    │
│                                                                 │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │                    Config (Config.java)                  │   │
│  │    Loads: .env file + config.properties                  │   │
│  └─────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────┘
```

### Package Structure

```
src/main/java/com/jarvis/
├── JarvisAssistant.java      # CLI Entry Point
├── ai/
│   └── AIProcessor.java      # AI Backend Switcher
├── commands/
│   ├── CommandHandler.java   # Main Command Router
│   ├── SystemCommands.java   # OS-level Commands
│   ├── WebCommands.java      # Web Browser Commands
│   └── AppAutomation.java    # GUI Automation
├── config/
│   └── Config.java           # Configuration Manager
├── gui/
│   ├── JarvisGUI.java        # Main GUI Window
│   └── AnimatedBackground.java
├── security/
│   ├── FileAnalyzer.java     # File Security Scanner
│   ├── LinkChecker.java      # URL Safety Checker
│   └── PentestingTools.java  # Kali Linux Tools
├── services/
│   └── WeatherService.java   # Weather API Integration
├── speech/
│   ├── SpeechRecognizer.java # Voice Input
│   └── TextToSpeech.java     # Voice Output
└── utils/
    ├── FuzzyMatcher.java     # Fuzzy String Matching
    └── NetworkChecker.java   # Network Status Detection
```

---

## 3. Installation & Setup

### Prerequisites

1. **Java 11+**: OpenJDK or Oracle JDK
   ```bash
   sudo apt install openjdk-11-jdk
   ```

2. **Maven**: Build tool
   ```bash
   sudo apt install maven
   ```

3. **Ollama** (for offline AI):
   ```bash
   curl -fsSL https://ollama.com/install.sh | sh
   ollama pull llama2
   ```

### Step-by-Step Setup

1. **Clone/Navigate to the project**:
   ```bash
   cd /home/saran304/Intelegence_Responsive_Integrate_System
   ```

2. **Configure API Keys** (optional for online mode):
   ```bash
   cp .env.example .env
   nano .env  # Add your API keys
   ```

3. **Build the project**:
   ```bash
   mvn clean compile
   ```

4. **Run the project**:
   ```bash
   # GUI Mode (recommended)
   mvn exec:java -Dexec.mainClass="com.jarvis.gui.JarvisGUI"
   
   # CLI Mode
   mvn exec:java -Dexec.mainClass="com.jarvis.JarvisAssistant"
   ```

---

## 4. Running the Project

### Method 1: Using Scripts

```bash
# Make scripts executable
chmod +x quick-start.sh start-jarvis-gui.sh start-jarvis.sh

# Run with GUI
./start-jarvis-gui.sh

# Run CLI version
./start-jarvis.sh
```

### Method 2: Using Maven

```bash
# GUI Mode
mvn clean compile exec:java -Dexec.mainClass="com.jarvis.gui.JarvisGUI"

# CLI Mode
mvn clean compile exec:java -Dexec.mainClass="com.jarvis.JarvisAssistant"
```

### Method 3: Create JAR and Run

```bash
mvn clean package
java -jar target/jarvis-voice-assistant-1.0.0.jar
```

---

## 5. Source Code Breakdown

### 5.1 JarvisAssistant.java (Main CLI Entry Point)

**Purpose**: Command-line interface for I.R.I.S.

```java
public class JarvisAssistant {
    private final TextToSpeech tts;           // Voice output
    private final SpeechRecognizer speechRecognizer;  // Voice input
    private final CommandHandler commandHandler;      // Process commands
    private final Config config;              // Configuration
    private boolean running;                  // Application state
}
```

**Key Methods**:
- `start()`: Initializes the assistant, checks microphone, starts main loop
- `runVoiceMode()`: Listens for wake word ("Jarvis", "Hey Jarvis"), then processes commands
- `runTextMode()`: Fallback text input mode when voice doesn't work
- `processCommand(String command)`: Routes command to handler, speaks response
- `shutdown()`: Clean up and exit

**Flow**:
1. Initialize components (TTS, Speech, CommandHandler)
2. Test microphone
3. Listen for wake word → Respond "Yes?" → Listen for command → Process → Speak response

---

### 5.2 JarvisGUI.java (GUI Entry Point)

**Purpose**: Modern graphical interface with animated background.

```java
public class JarvisGUI extends JFrame {
    // Core components
    private final CommandHandler commandHandler;
    private final TextToSpeech tts;
    private final SpeechRecognizer speechRecognizer;
    private final AIProcessor aiProcessor;
    
    // UI Components
    private JTextArea chatArea;      // Chat display
    private JTextField inputField;   // Text input
    private JButton voiceButton;     // Push-to-talk button
    private JButton aiModeButton;    // Toggle AI mode
}
```

**Key Features**:
- **Theme**: Dark green (JARVIS-style) with particle animation background
- **Input Methods**: Text field + Voice button (Ctrl+Space shortcut)
- **AI Toggle**: Switch between Gemini/Ollama manually
- **Network Status**: Real-time display of online/offline status

**Flow**:
1. User types or uses voice input
2. `sendMessage()` → CommandHandler → AI Response
3. Response displayed in chat + spoken aloud
4. Network status updated every 30 seconds

---

### 5.3 AIProcessor.java (AI Brain)

**Purpose**: Manages multiple AI backends with automatic fallback.

```java
public class AIProcessor {
    private final OkHttpClient httpClient;
    private final NetworkChecker networkChecker;
    private final String geminiApiKey;
    private final String grokApiKey;
    
    private boolean manualModeEnabled = false;
    private String manualModeSelection = "auto";  // "gemini", "grok", "ollama"
}
```

**AI Mode Selection Logic**:
```
if (manualModeEnabled) {
    return manualModeSelection;  // User's choice
} else if (networkChecker.isOnline() && networkChecker.isFastNetwork()) {
    return currentOnlineAI;  // Gemini or Grok (round-robin)
} else {
    return "ollama";  // Offline fallback
}
```

**Key Methods**:
- `processQuery(String query)`: Main entry point - selects AI and processes
- `processWithOllama(String query)`: Local AI via Ollama API
- `processWithGemini(String query)`: Google Gemini API
- `processWithGrok(String query)`: X.AI Grok API

**Ollama Request Format**:
```json
{
  "model": "llama2",
  "prompt": "You are JARVIS... User query: {query}",
  "stream": false
}
```

---

### 5.4 CommandHandler.java (Command Router)

**Purpose**: Routes user commands to appropriate handlers.

**Command Categories**:

| Category | Trigger Words | Handler |
|----------|---------------|---------|
| Time/Date | "time", "date" | `getTime()`, `getDate()` |
| Open App | "open" | `handleOpenCommand()` |
| Volume | "volume" | `handleVolumeCommand()` |
| Power | "shutdown", "restart", "sleep" | `handlePowerCommand()` |
| Web Search | "google", "youtube", "wikipedia" | WebCommands |
| Weather | "weather" | WeatherService |
| Security | "hack", "crack", "scan" | `handleSecurityQuery()` |
| File Analysis | "analyze file", "scan file" | FileAnalyzer |
| Exit | "exit", "quit", "goodbye" | returns "exit" |
| Default | Any other query | AIProcessor |

**Command Flow**:
```java
public String processCommand(String command) {
    command = command.toLowerCase().trim();
    
    if (isComplexCommand(command)) {
        return handleComplexCommand(command);  // Multi-step with AI
    }
    
    if (command.contains("open")) {
        return handleOpenCommand(command);
    }
    // ... more checks ...
    
    // Default: Ask AI
    return aiProcessor.processQuery(command);
}
```

---

### 5.5 Config.java (Configuration Manager)

**Purpose**: Loads settings from `.env` and `config.properties`.

**Sources**:
1. `.env` - API keys and secrets
2. `src/main/resources/config.properties` - Application settings

**Configuration Options**:

| Setting | Source | Default | Description |
|---------|--------|---------|-------------|
| `GEMINI_API_KEY` | .env | - | Google Gemini API key |
| `GROK_API_KEY` | .env | - | X.AI Grok API key |
| `OPENWEATHER_API_KEY` | .env | - | Weather API key |
| `speech.offline.mode` | config.properties | false | Use Vosk offline |
| `speech.vosk.model.path` | config.properties | models/vosk-... | Vosk model location |
| `speech.rate` | config.properties | 150 | TTS words per minute |
| `wake.words` | config.properties | jarvis,hey jarvis | Activation phrases |
| `system.browser` | config.properties | firefox | Default browser |
| `ai.ollama.model` | config.properties | llama2 | Ollama model name |

---

### 5.6 SpeechRecognizer.java (Voice Input)

**Purpose**: Captures audio and converts to text.

**Modes**:
1. **Offline (Vosk)**: Uses downloaded Vosk model locally
2. **Online (Google Cloud)**: Uses Google Speech-to-Text API

**Audio Settings**:
```java
audioFormat = new AudioFormat(
    16000.0f,  // 16kHz sample rate
    16,        // 16-bit samples
    1,         // Mono
    true,      // Signed
    false      // Little endian
);
```

**Key Methods**:
- `listen(int seconds)`: Capture audio for N seconds, return text
- `listenWithCallback(int seconds, callback)`: Real-time audio level feedback
- `testMicrophone()`: Verify microphone is working
- `containsWakeWord(String text)`: Check for "jarvis" or "hey jarvis"

**Vosk Model Download**:
```java
// Auto-downloads if not present
if (!modelDir.exists()) {
    downloadVoskModel(modelPath);  // Downloads ~40MB model
}
```

---

### 5.7 TextToSpeech.java (Voice Output)

**Purpose**: Converts text to spoken audio using FreeTTS.

```java
public class TextToSpeech {
    private Voice voice;
    
    // Initialize FreeTTS voice
    voice = voiceManager.getVoice("kevin16");
    voice.allocate();
    voice.setRate(150);  // Words per minute
    voice.setVolume(1.0f);
}
```

**Key Methods**:
- `speak(String text)`: Async speech (non-blocking)
- `speakSync(String text)`: Sync speech (blocking)
- `setRate(int wpm)`: Adjust speech speed
- `shutdown()`: Free resources

---

### 5.8 SystemCommands.java (OS Automation)

**Purpose**: Open applications, control volume, system info.

**Application Mapping**:
```java
// Fuzzy matching for applications
APP_MAPPINGS = {
    {"firefox", "fire fox", "mozilla"},
    {"chrome", "google chrome"},
    {"terminal", "gnome-terminal", "console"},
    {"wireshark", "wire shark"},
    {"metasploit", "msfconsole", "msf"},
    // ... more mappings
}
```

**Volume Control**:
```bash
# PulseAudio (default)
pactl set-sink-volume @DEFAULT_SINK@ +10%
pactl set-sink-volume @DEFAULT_SINK@ -10%
pactl set-sink-mute @DEFAULT_SINK@ toggle
```

---

### 5.9 Security Tools

#### FileAnalyzer.java
- Calculates MD5, SHA1, SHA256 hashes
- Scans with ClamAV if available
- Pattern-based malware detection
- AI-powered file summary

#### LinkChecker.java
- URL safety analysis
- Redirect chain detection
- SSL certificate validation

#### PentestingTools.java
- Nmap network scanning
- Payload generation (Metasploit)
- Password cracking (John the Ripper, Hydra)
- SQL injection testing (SQLMap)
- Directory enumeration (Gobuster)

---

### 5.10 NetworkChecker.java (Network Detection)

**Purpose**: Determines if online and fast enough for cloud AI.

```java
// Check logic
if (pingHost("8.8.8.8") || pingHost("1.1.1.1")) {
    return true;  // Online
}

// Speed check
if (responseTime < 1000ms) {
    return "fast";  // Use Gemini/Grok
} else {
    return "slow";  // Use Ollama
}
```

**Caching**: Results cached for 30 seconds to avoid constant checks.

---

## 6. Configuration Guide

### .env File

```bash
# Required for online AI mode
GEMINI_API_KEY=your_gemini_api_key_here
GROK_API_KEY=your_grok_api_key_here
GROK_API_URL=https://api.x.ai/v1/chat/completions
GROK_MODEL=grok-beta

# Optional: Weather
OPENWEATHER_API_KEY=your_weather_key

# Optional: Google Speech (for online voice recognition)
GOOGLE_APPLICATION_CREDENTIALS=/path/to/credentials.json
```

### config.properties

```properties
# Speech settings
speech.rate=150
speech.volume=1.0
speech.voice=kevin16

# Wake words
wake.words=jarvis,hey jarvis,iris,hey iris

# Offline speech recognition
speech.offline.mode=true
speech.vosk.model.path=models/vosk-model-small-en-us-0.15

# System
system.browser=firefox
system.volume.command=pactl

# Offline AI
ai.offline.mode=false
ai.ollama.url=http://localhost:11434/api/generate
ai.ollama.model=llama2
```

---

## 7. Features & Commands

### Voice/Text Commands

| Command | Action |
|---------|--------|
| "What time is it?" | Tells current time |
| "What's the date?" | Tells current date |
| "Open Firefox" | Opens Firefox browser |
| "Open terminal" | Opens terminal |
| "Volume up/down" | Adjusts system volume |
| "Mute" | Toggles mute |
| "Search Google for cats" | Opens Google search |
| "Play music on YouTube" | Opens YouTube search |
| "Weather in London" | Gets weather info |
| "Take a screenshot" | Captures screen |
| "Analyze file /path/to/file" | Security analysis |
| "Check link example.com" | URL safety check |
| "Scan network 192.168.1.0/24" | Nmap scan |
| "Exit" / "Goodbye" | Shuts down |

### AI Queries

Any question not matching specific commands goes to AI:
- "Explain quantum computing"
- "Write a Python hello world"
- "How do I hack WiFi in Kali Linux?"

---

## 8. Troubleshooting

### Issue: "Permission denied" when running scripts

```bash
chmod +x quick-start.sh start-jarvis-gui.sh start-jarvis.sh
```

### Issue: Microphone not working

```bash
# Check if PulseAudio recognizes mic
pactl list sources short

# Increase microphone volume
pactl set-source-volume @DEFAULT_SOURCE@ 150%

# Test recording
arecord -d 5 test.wav && aplay test.wav
```

### Issue: Ollama not responding

```bash
# Start Ollama server
ollama serve

# Check if model is installed
ollama list

# Pull a model if needed
ollama pull llama2
```

### Issue: Vosk model download fails

```bash
# Manual download
cd models
wget https://alphacephei.com/vosk/models/vosk-model-small-en-us-0.15.zip
unzip vosk-model-small-en-us-0.15.zip
```

### Issue: GUI doesn't start

```bash
# Check Java version
java -version

# Run with debug output
mvn exec:java -Dexec.mainClass="com.jarvis.gui.JarvisGUI" -X
```

---

## Quick Reference

### Run Commands

```bash
# Quick start GUI
mvn exec:java -Dexec.mainClass="com.jarvis.gui.JarvisGUI"

# CLI mode
mvn exec:java -Dexec.mainClass="com.jarvis.JarvisAssistant"

# Build JAR
mvn clean package

# Clean build
mvn clean
```

### Important Files

| File | Purpose |
|------|---------|
| `pom.xml` | Maven dependencies & build config |
| `.env` | API keys (create from .env.example) |
| `src/main/resources/config.properties` | App settings |
| `models/` | Vosk speech model directory |

---

*Documentation generated: December 2024*
*Project: I.R.I.S (Intelligent Responsive Integrated System)*
