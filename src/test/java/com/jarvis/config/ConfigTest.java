package com.jarvis.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for Config class
 */
class ConfigTest {
    
    private Config config;
    
    @BeforeEach
    void setUp() {
        config = Config.getInstance();
    }
    
    @Test
    void testGetInstance() {
        Config instance1 = Config.getInstance();
        Config instance2 = Config.getInstance();
        assertNotNull(instance1);
        assertSame(instance1, instance2, "Config should be a singleton");
    }
    
    @Test
    void testDefaultSpeechRate() {
        int rate = config.getSpeechRate();
        assertTrue(rate > 0, "Speech rate should be positive");
        assertTrue(rate >= 50 && rate <= 300, "Speech rate should be reasonable");
    }
    
    @Test
    void testDefaultSpeechVolume() {
        float volume = config.getSpeechVolume();
        assertTrue(volume >= 0.0f && volume <= 2.0f, "Volume should be between 0 and 2");
    }
    
    @Test
    void testDefaultVoiceType() {
        String voice = config.getVoiceType();
        assertNotNull(voice);
        assertFalse(voice.isEmpty(), "Voice type should not be empty");
    }
    
    @Test
    void testGetWakeWords() {
        String[] wakeWords = config.getWakeWords();
        assertNotNull(wakeWords);
        assertTrue(wakeWords.length > 0, "Should have at least one wake word");
    }
    
    @Test
    void testGetBrowserCommand() {
        String browser = config.getBrowserCommand();
        assertNotNull(browser);
        assertFalse(browser.isEmpty(), "Browser command should not be empty");
    }
    
    @Test
    void testGetPropertyWithDefault() {
        String nonExistent = config.getProperty("non.existent.key", "default");
        assertEquals("default", nonExistent, "Should return default for non-existent property");
    }
    
    @Test
    void testVoskModelPath() {
        String modelPath = config.getVoskModelPath();
        assertNotNull(modelPath);
        assertTrue(modelPath.contains("vosk"), "Model path should reference vosk");
    }
}
