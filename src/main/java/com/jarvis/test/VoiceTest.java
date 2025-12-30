package com.jarvis.test;

import com.jarvis.speech.SpeechRecognizer;
import javax.sound.sampled.*;

/**
 * Test program to diagnose voice recognition issues
 */
public class VoiceTest {
    
    public static void main(String[] args) {
        System.out.println("=".repeat(60));
        System.out.println("JARVIS Voice Recognition Test");
        System.out.println("=".repeat(60));
        
        // Test 1: Check microphone availability
        System.out.println("\n[Test 1] Checking microphone availability...");
        testMicrophoneAvailability();
        
        // Test 2: Test audio capture
        System.out.println("\n[Test 2] Testing audio capture (5 seconds)...");
        System.out.println("Please speak something...");
        testAudioCapture();
        
        // Test 3: Test Vosk model loading
        System.out.println("\n[Test 3] Testing Vosk model loading...");
        testVoskModel();
        
        // Test 4: Full voice recognition test
        System.out.println("\n[Test 4] Full voice recognition test...");
        System.out.println("Say something (you have 5 seconds)...");
        testFullRecognition();
        
        System.out.println("\n" + "=".repeat(60));
        System.out.println("Test completed!");
        System.out.println("=".repeat(60));
    }
    
    private static void testMicrophoneAvailability() {
        try {
            AudioFormat format = new AudioFormat(16000.0f, 16, 1, true, false);
            DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);
            
            if (AudioSystem.isLineSupported(info)) {
                System.out.println("✅ Microphone is supported");
                
                // List all available mixers
                Mixer.Info[] mixers = AudioSystem.getMixerInfo();
                System.out.println("\nAvailable audio devices:");
                for (int i = 0; i < mixers.length; i++) {
                    System.out.println("  " + (i+1) + ". " + mixers[i].getName());
                }
            } else {
                System.out.println("❌ Microphone is NOT supported");
            }
        } catch (Exception e) {
            System.out.println("❌ Error checking microphone: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private static void testAudioCapture() {
        try {
            AudioFormat format = new AudioFormat(16000.0f, 16, 1, true, false);
            DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);
            TargetDataLine line = (TargetDataLine) AudioSystem.getLine(info);
            
            line.open(format);
            line.start();
            
            System.out.println("🎤 Recording...");
            
            byte[] buffer = new byte[4096];
            long totalBytes = 0;
            long maxAmplitude = 0;
            long endTime = System.currentTimeMillis() + 5000;
            
            while (System.currentTimeMillis() < endTime) {
                int bytesRead = line.read(buffer, 0, buffer.length);
                totalBytes += bytesRead;
                
                // Calculate amplitude
                for (int i = 0; i < bytesRead - 1; i += 2) {
                    int sample = (buffer[i+1] << 8) | (buffer[i] & 0xFF);
                    maxAmplitude = Math.max(maxAmplitude, Math.abs(sample));
                }
            }
            
            line.stop();
            line.close();
            
            System.out.println("✅ Audio capture successful");
            System.out.println("   Total bytes captured: " + totalBytes);
            System.out.println("   Max amplitude: " + maxAmplitude);
            
            if (maxAmplitude < 100) {
                System.out.println("⚠️  WARNING: Very low audio level. Microphone may be muted or not working.");
            } else {
                System.out.println("✅ Audio level looks good!");
            }
            
        } catch (Exception e) {
            System.out.println("❌ Error capturing audio: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private static void testVoskModel() {
        try {
            System.out.println("Loading Vosk model from: models/vosk-model-small-en-us-0.15");
            org.vosk.Model model = new org.vosk.Model("models/vosk-model-small-en-us-0.15");
            System.out.println("✅ Vosk model loaded successfully");
            model.close();
        } catch (Exception e) {
            System.out.println("❌ Error loading Vosk model: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private static void testFullRecognition() {
        try {
            SpeechRecognizer recognizer = new SpeechRecognizer();
            String result = recognizer.listen(5);
            
            if (result != null && !result.isEmpty()) {
                System.out.println("✅ Recognition successful!");
                System.out.println("   You said: \"" + result + "\"");
            } else {
                System.out.println("❌ No speech recognized");
            }
            
            recognizer.shutdown();
        } catch (Exception e) {
            System.out.println("❌ Error in full recognition test: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
