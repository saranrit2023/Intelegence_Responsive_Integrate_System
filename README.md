# JARVIS Voice Assistant

A sophisticated AI-powered voice assistant for Linux, built with Java, inspired by Iron Man's JARVIS. This assistant can understand voice commands, respond with natural speech, control your system, search the web, and have intelligent conversations.

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

## 🚀 Quick Start Options

### Option 1: Offline Mode (Recommended for Privacy)
**No internet or API keys required!**

```bash
./setup-offline.sh
```

This will:
- Install Ollama (local LLM)
- Download an AI model (llama2, mistral, or phi)
- Configure JARVIS for offline use
- Build the project

Then run:
```bash
java -jar target/jarvis-voice-assistant-1.0.0.jar
```

**See [OFFLINE_MODE.md](OFFLINE_MODE.md) for detailed offline setup guide.**

### Option 2: Online Mode (Cloud AI)
Uses Google Gemini API for AI (requires API key and internet).

```bash
./setup.sh
```

Then configure your API keys in `.env` file.

---

## 📋 Prerequisites

- **Java 11 or higher**
- **Maven 3.6+**
- **Linux OS** (tested on Ubuntu/Debian-based systems)
- **Microphone** (for voice input)
- **Internet connection** (for API services)

## 🔧 Installation Guide

### 1. Clone or Download the Project

```bash
cd /home/saran304/Jarvis_voice_controler
```

### 2. Install System Dependencies

For audio support on Ubuntu/Debian:

```bash
sudo apt-get update
sudo apt-get install -y openjdk-11-jdk maven pulseaudio
```

For Arch Linux Users:

```bash
sudo pacman -S jdk11-openjdk maven pulseaudio
# For Ollama (install via AUR or manual download)
yay -S ollama # Example using yay AUR helper
```


### 3. Get API Keys

#### Google Gemini API (Required for AI features)
1. Visit [Google AI Studio](https://makersuite.google.com/app/apikey)
2. Create a new API key
3. Copy the key for later use

#### Google Cloud Speech-to-Text (Required for voice recognition)
1. Visit [Google Cloud Console](https://console.cloud.google.com/)
2. Create a new project
3. Enable the "Cloud Speech-to-Text API"
4. Create a service account and download the JSON credentials file
5. Save the path to this file

#### OpenWeatherMap API (Optional, for weather features)
1. Visit [OpenWeatherMap](https://openweathermap.org/api)
2. Sign up for a free account
3. Get your API key from the dashboard

### 4. Configure Environment Variables

Create a `.env` file in the project root:

```bash
cp .env.example .env
```

Edit `.env` and add your API keys:

```bash
GEMINI_API_KEY=your_actual_gemini_api_key
OPENWEATHER_API_KEY=your_actual_openweather_api_key
GOOGLE_APPLICATION_CREDENTIALS=/absolute/path/to/google-credentials.json
```

### 5. Build the Project

```bash
mvn clean install
```

## 🚀 Usage

### Running with Maven

```bash
mvn exec:java -Dexec.mainClass="com.jarvis.JarvisAssistant"
```

### Running the JAR

```bash
java -jar target/jarvis-voice-assistant-1.0.jar
```

### Text Mode (No Voice Recognition)

If voice recognition is not available or you prefer text input, the assistant will automatically switch to text mode. You can also type `text` during voice mode to switch.

## 🎯 Command Examples

### System Commands
- "Open Firefox"
- "Open terminal"
- "Volume up"
- "Volume down"
- "Mute volume"

### Web Commands
- "Search Google for Java tutorials"
- "Play Bohemian Rhapsody on YouTube"
- "Wikipedia artificial intelligence"

### Information Queries
- "What time is it?"
- "What's the date?"
- "What's the weather?"
- "Weather in New York"

### AI Conversations
- "Tell me a joke"
- "What is quantum computing?"
- "Explain machine learning"
- "Who won the World Cup in 2022?"

### Exit
- "Exit"
- "Quit"
- "Goodbye"

## 📁 Project Structure

```
Jarvis_voice_controler/
├── pom.xml                          # Maven configuration
├── .env                             # Environment variables (create from .env.example)
├── .env.example                     # Environment template
├── README.md                        # This file
└── src/
    └── main/
        ├── java/
        │   └── com/
        │       └── jarvis/
        │           ├── JarvisAssistant.java      # Main application
        │           ├── config/
        │           │   └── Config.java           # Configuration management
        │           ├── speech/
        │           │   ├── SpeechRecognizer.java # Voice recognition
        │           │   └── TextToSpeech.java     # Voice synthesis
        │           ├── commands/
        │           │   ├── CommandHandler.java   # Command routing
        │           │   ├── SystemCommands.java   # System operations
        │           │   └── WebCommands.java      # Web operations
        │           ├── ai/
        │           │   └── AIProcessor.java      # Gemini AI integration
        │           └── services/
        │               └── WeatherService.java   # Weather API
        └── resources/
            └── config.properties                  # Application settings
```

## ⚙️ Configuration

Edit `src/main/resources/config.properties` to customize:

- **Speech settings**: Rate, volume, voice type
- **Wake words**: Customize activation phrases
- **System commands**: Default browser, volume control method
- **Default location**: For weather queries

## 🐛 Troubleshooting

### Voice Recognition Not Working

1. **Check Google Cloud credentials**:
   ```bash
   echo $GOOGLE_APPLICATION_CREDENTIALS
   ```
   Make sure the path is correct and the file exists.

2. **Test microphone**:
   ```bash
   arecord -d 5 test.wav
   aplay test.wav
   ```

3. **Use text mode**: Type `text` or the assistant will auto-switch if voice fails.

### No Audio Output

1. **Check PulseAudio**:
   ```bash
   pulseaudio --check
   pulseaudio --start
   ```

2. **Test system audio**:
   ```bash
   speaker-test -t wav -c 2
   ```

### API Errors

1. **Verify API keys** in `.env` file
2. **Check internet connection**
3. **Verify API quotas** in respective dashboards

### Build Errors

1. **Clean and rebuild**:
   ```bash
   mvn clean install -U
   ```

2. **Check Java version**:
   ```bash
   java -version  # Should be 11 or higher
   ```

## 🔒 Security Notes

- **Never commit your `.env` file** to version control
- **Keep API keys secure** and rotate them regularly
- **Review system commands** before enabling shutdown/restart features
- **Limit permissions** for the application user

## 📝 License

This project is provided as-is for educational and personal use.

## 🤝 Contributing

Feel free to fork, modify, and enhance this project! Some ideas for improvements:

- Add more voice commands
- Implement conversation context
- Add calendar integration
- Support for multiple languages
- Custom wake word training
- Integration with smart home devices

## 📧 Support

For issues or questions:
1. Check the troubleshooting section
2. Review the configuration files
3. Check API documentation for respective services

## 🎉 Acknowledgments

- Google Cloud Speech-to-Text API
- Google Gemini AI
- FreeTTS for text-to-speech
- OpenWeatherMap API
- Inspired by Marvel's JARVIS

---

**Enjoy your personal JARVIS assistant! 🚀**
