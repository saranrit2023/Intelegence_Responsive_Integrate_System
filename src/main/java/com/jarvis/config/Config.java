package com.jarvis.config;

import io.github.cdimascio.dotenv.Dotenv;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Configuration management for JARVIS Voice Assistant
 */
public class Config {
    private static Config instance;
    private final Properties properties;
    private final Dotenv dotenv;
    
    // API Keys
    private String geminiApiKey;
    private String grokApiKey;
    private String grokApiUrl;
    private String grokModel;
    private String openWeatherApiKey;
    private String googleCredentialsPath;
    
    // Voice Settings
    private int speechRate;
    private float speechVolume;
    private String voiceType;
    
    // Wake Words
    private String[] wakeWords;
    
    // System Settings
    private String browserCommand;
    private String volumeCommand;
    
    // Speech Recognition Settings
    private boolean speechOfflineMode;
    private String voskModelPath;
    private String voskModelUrl;
    
    private Config() {
        properties = new Properties();
        dotenv = Dotenv.configure().ignoreIfMissing().load();
        loadConfiguration();
    }
    
    public static synchronized Config getInstance() {
        if (instance == null) {
            instance = new Config();
        }
        return instance;
    }
    
    private void loadConfiguration() {
        // Load from config.properties
        try (InputStream input = getClass().getClassLoader()
                .getResourceAsStream("config.properties")) {
            if (input != null) {
                properties.load(input);
            }
        } catch (IOException e) {
            System.err.println("Warning: Could not load config.properties: " + e.getMessage());
        }
        
        // Load API keys from environment
        geminiApiKey = dotenv.get("GEMINI_API_KEY", "");
        grokApiKey = dotenv.get("GROK_API_KEY", "");
        grokApiUrl = dotenv.get("GROK_API_URL", "https://api.x.ai/v1/chat/completions");
        grokModel = dotenv.get("GROK_MODEL", "grok-beta");
        openWeatherApiKey = dotenv.get("OPENWEATHER_API_KEY", "");
        googleCredentialsPath = dotenv.get("GOOGLE_APPLICATION_CREDENTIALS", "");
        
        // Load voice settings
        speechRate = Integer.parseInt(properties.getProperty("speech.rate", "150"));
        speechVolume = Float.parseFloat(properties.getProperty("speech.volume", "1.0"));
        voiceType = properties.getProperty("speech.voice", "kevin16");
        
        // Load wake words
        String wakeWordsStr = properties.getProperty("wake.words", "jarvis,hey jarvis");
        wakeWords = wakeWordsStr.split(",");
        
        // Load system settings
        browserCommand = properties.getProperty("system.browser", "firefox");
        volumeCommand = properties.getProperty("system.volume.command", "pactl");
        
        // Load speech recognition settings
        speechOfflineMode = Boolean.parseBoolean(properties.getProperty("speech.offline.mode", "false"));
        voskModelPath = properties.getProperty("speech.vosk.model.path", "models/vosk-model-small-en-us-0.15");
        voskModelUrl = properties.getProperty("speech.vosk.model.url", "https://alphacephei.com/vosk/models/vosk-model-small-en-us-0.15.zip");
    }
    
    // Getters
    public String getGeminiApiKey() { return geminiApiKey; }
    public String getGrokApiKey() { return grokApiKey; }
    public String getGrokApiUrl() { return grokApiUrl; }
    public String getGrokModel() { return grokModel; }
    public String getOpenWeatherApiKey() { return openWeatherApiKey; }
    public String getGoogleCredentialsPath() { return googleCredentialsPath; }
    public int getSpeechRate() { return speechRate; }
    public float getSpeechVolume() { return speechVolume; }
    public String getVoiceType() { return voiceType; }
    public String[] getWakeWords() { return wakeWords; }
    public String getBrowserCommand() { return browserCommand; }
    public String getVolumeCommand() { return volumeCommand; }
    public boolean isSpeechOfflineMode() { return speechOfflineMode; }
    public String getVoskModelPath() { return voskModelPath; }
    public String getVoskModelUrl() { return voskModelUrl; }
    
    public String getProperty(String key, String defaultValue) {
        return properties.getProperty(key, defaultValue);
    }
}
