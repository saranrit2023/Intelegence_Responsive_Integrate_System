package com.jarvis.speech;

import com.jarvis.config.Config;
import com.sun.speech.freetts.Voice;
import com.sun.speech.freetts.VoiceManager;

/**
 * Text-to-Speech engine using FreeTTS
 */
public class TextToSpeech {
    private Voice voice;
    private final Config config;
    
    public TextToSpeech() {
        this.config = Config.getInstance();
        initializeVoice();
    }
    
    private void initializeVoice() {
        System.setProperty("freetts.voices", 
            "com.sun.speech.freetts.en.us.cmu_us_kal.KevinVoiceDirectory");
        
        VoiceManager voiceManager = VoiceManager.getInstance();
        voice = voiceManager.getVoice(config.getVoiceType());
        
        if (voice == null) {
            // Fallback to default voice
            voice = voiceManager.getVoice("kevin16");
        }
        
        if (voice != null) {
            voice.allocate();
            voice.setRate(config.getSpeechRate());
            voice.setVolume(config.getSpeechVolume());
        } else {
            System.err.println("Error: Could not initialize text-to-speech voice");
        }
    }
    
    /**
     * Speak the given text
     * @param text Text to speak
     */
    public void speak(String text) {
        if (voice != null && text != null && !text.isEmpty()) {
            System.out.println("I.R.I.S: " + text);
            new Thread(() -> {
                voice.speak(text);
            }).start();
        }
    }
    
    /**
     * Speak the given text synchronously (wait for completion)
     * @param text Text to speak
     */
    public void speakSync(String text) {
        if (voice != null && text != null && !text.isEmpty()) {
            System.out.println("I.R.I.S: " + text);
            voice.speak(text);
        }
    }
    
    /**
     * Set speech rate
     * @param rate Words per minute
     */
    public void setRate(int rate) {
        if (voice != null) {
            voice.setRate(rate);
        }
    }
    
    /**
     * Set speech volume
     * @param volume Volume level (0.0 to 1.0)
     */
    public void setVolume(float volume) {
        if (voice != null) {
            voice.setVolume(volume);
        }
    }
    
    /**
     * Clean up resources
     */
    public void shutdown() {
        if (voice != null) {
            voice.deallocate();
        }
    }
}
