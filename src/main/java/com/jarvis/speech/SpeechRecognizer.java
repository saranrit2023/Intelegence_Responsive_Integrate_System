package com.jarvis.speech;

import com.google.cloud.speech.v1.*;
import com.google.protobuf.ByteString;
import com.jarvis.config.Config;
import org.vosk.Model;
import org.vosk.Recognizer;
import org.vosk.LibVosk;

import javax.sound.sampled.*;
import java.io.*;
import java.net.URL;
import java.nio.file.*;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Speech recognition using Vosk (offline) or Google Cloud Speech-to-Text API (online)
 * Supports automatic model download and fallback mechanisms
 */
public class SpeechRecognizer {
    private final Config config;
    private SpeechClient speechClient;
    private Model voskModel;
    private AudioFormat audioFormat;
    private volatile float currentAudioLevel = 0.0f;
    private volatile boolean isListening = false;
    
    // Callback interface for real-time audio level updates
    public interface AudioLevelCallback {
        void onAudioLevel(float level);
        void onTranscript(String text);
    }
    
    public SpeechRecognizer() {
        this.config = Config.getInstance();
        setupAudioFormat();
        
        // Initialize based on offline mode setting
        if (config.isSpeechOfflineMode()) {
            System.out.println("🎤 Speech Recognition: OFFLINE mode (Vosk)");
            initializeVoskModel();
        } else {
            System.out.println("🎤 Speech Recognition: ONLINE mode (Google Cloud)");
            initializeSpeechClient();
        }
    }
    
    /**
     * Initialize Vosk model for offline recognition
     */
    private void initializeVoskModel() {
        try {
            String modelPath = config.getVoskModelPath();
            File modelDir = new File(modelPath);
            
            // Check if model exists, if not download it
            if (!modelDir.exists() || !modelDir.isDirectory()) {
                System.out.println("📥 Vosk model not found. Downloading...");
                downloadVoskModel(modelPath);
            }
            
            voskModel = new Model(modelPath);
            System.out.println("✅ Vosk model loaded successfully from: " + modelPath);
        } catch (Exception e) {
            System.err.println("❌ Warning: Could not initialize Vosk model: " + e.getMessage());
            System.err.println("Falling back to Google Cloud Speech API if available...");
            initializeSpeechClient();
        }
    }
    
    /**
     * Download and extract Vosk model
     */
    private void downloadVoskModel(String modelPath) {
        try {
            String modelUrl = config.getVoskModelUrl();
            System.out.println("Downloading from: " + modelUrl);
            
            // Create models directory
            File modelsDir = new File("models");
            if (!modelsDir.exists()) {
                modelsDir.mkdirs();
            }
            
            // Download zip file
            File zipFile = new File("models/vosk-model.zip");
            try (InputStream in = new URL(modelUrl).openStream();
                 FileOutputStream out = new FileOutputStream(zipFile)) {
                
                byte[] buffer = new byte[8192];
                int bytesRead;
                long totalBytes = 0;
                
                while ((bytesRead = in.read(buffer)) != -1) {
                    out.write(buffer, 0, bytesRead);
                    totalBytes += bytesRead;
                    if (totalBytes % (1024 * 1024) == 0) {
                        System.out.print(".");
                    }
                }
                System.out.println("\n✅ Download complete!");
            }
            
            // Extract zip file
            System.out.println("📦 Extracting model...");
            unzip(zipFile.getAbsolutePath(), modelsDir.getAbsolutePath());
            
            // Delete zip file
            zipFile.delete();
            
            System.out.println("✅ Model extracted successfully!");
        } catch (Exception e) {
            System.err.println("❌ Error downloading Vosk model: " + e.getMessage());
            throw new RuntimeException("Failed to download Vosk model", e);
        }
    }
    
    /**
     * Unzip a file
     */
    private void unzip(String zipFilePath, String destDir) throws IOException {
        File dir = new File(destDir);
        if (!dir.exists()) dir.mkdirs();
        
        try (ZipInputStream zipIn = new ZipInputStream(new FileInputStream(zipFilePath))) {
            ZipEntry entry = zipIn.getNextEntry();
            
            while (entry != null) {
                String filePath = destDir + File.separator + entry.getName();
                
                if (!entry.isDirectory()) {
                    extractFile(zipIn, filePath);
                } else {
                    File dirEntry = new File(filePath);
                    dirEntry.mkdirs();
                }
                
                zipIn.closeEntry();
                entry = zipIn.getNextEntry();
            }
        }
    }
    
    /**
     * Extract a single file from zip
     */
    private void extractFile(ZipInputStream zipIn, String filePath) throws IOException {
        try (BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(filePath))) {
            byte[] buffer = new byte[4096];
            int read;
            while ((read = zipIn.read(buffer)) != -1) {
                bos.write(buffer, 0, read);
            }
        }
    }
    
    /**
     * Initialize Google Cloud Speech client (fallback)
     */
    private void initializeSpeechClient() {
        try {
            String credPath = config.getGoogleCredentialsPath();
            if (credPath != null && !credPath.isEmpty()) {
                System.setProperty("GOOGLE_APPLICATION_CREDENTIALS", credPath);
            }
            speechClient = SpeechClient.create();
            System.out.println("✅ Google Cloud Speech client initialized");
        } catch (Exception e) {
            System.err.println("⚠️  Warning: Could not initialize Google Speech client: " + e.getMessage());
            System.err.println("Speech recognition may not work without Vosk model or Google credentials");
        }
    }
    
    private void setupAudioFormat() {
        audioFormat = new AudioFormat(
            16000.0f,  // Sample rate
            16,        // Sample size in bits
            1,         // Channels (mono)
            true,      // Signed
            false      // Little endian
        );
    }
    
    /**
     * Listen for audio input and convert to text
     * @param durationSeconds How long to listen
     * @return Recognized text
     */
    public String listen(int durationSeconds) {
        try {
            byte[] audioData = captureAudio(durationSeconds);
            return recognizeSpeech(audioData);
        } catch (Exception e) {
            System.err.println("Error during speech recognition: " + e.getMessage());
            return "";
        }
    }
    
    /**
     * Capture audio from microphone
     */
    private byte[] captureAudio(int durationSeconds) throws LineUnavailableException {
        DataLine.Info info = new DataLine.Info(TargetDataLine.class, audioFormat);
        
        if (!AudioSystem.isLineSupported(info)) {
            System.err.println("Audio line not supported");
            return new byte[0];
        }
        
        TargetDataLine targetLine = (TargetDataLine) AudioSystem.getLine(info);
        targetLine.open(audioFormat);
        targetLine.start();
        
        System.out.println("Listening...");
        
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[4096];
        
        long endTime = System.currentTimeMillis() + (durationSeconds * 1000);
        
        while (System.currentTimeMillis() < endTime) {
            int bytesRead = targetLine.read(buffer, 0, buffer.length);
            out.write(buffer, 0, bytesRead);
        }
        
        targetLine.stop();
        targetLine.close();
        
        return out.toByteArray();
    }
    
    /**
     * Recognize speech from audio data
     * Uses Vosk if available and offline mode is enabled, otherwise uses Google Cloud API
     */
    private String recognizeSpeech(byte[] audioData) {
        if (audioData.length == 0) {
            return "";
        }
        
        // Try Vosk first if available
        if (voskModel != null) {
            return recognizeSpeechVosk(audioData);
        }
        
        // Fallback to Google Cloud API
        if (speechClient != null) {
            return recognizeSpeechGoogle(audioData);
        }
        
        System.err.println("No speech recognition engine available!");
        return "";
    }
    
    /**
     * Recognize speech using Vosk (offline)
     */
    private String recognizeSpeechVosk(byte[] audioData) {
        try (Recognizer recognizer = new Recognizer(voskModel, 16000)) {
            recognizer.acceptWaveForm(audioData, audioData.length);
            String result = recognizer.getFinalResult();
            
            // Parse JSON result
            // Vosk returns: {"text":"hello world"}
            String text = extractTextFromVoskResult(result);
            
            if (!text.isEmpty()) {
                System.out.println("You said: " + text);
                return text.toLowerCase();
            }
        } catch (Exception e) {
            System.err.println("Error in Vosk recognition: " + e.getMessage());
        }
        
        return "";
    }
    
    /**
     * Extract text from Vosk JSON result
     */
    private String extractTextFromVoskResult(String jsonResult) {
        try {
            // Simple JSON parsing for {"text":"..."}
            int textStart = jsonResult.indexOf("\"text\"");
            if (textStart == -1) return "";
            
            int colonIndex = jsonResult.indexOf(":", textStart);
            int quoteStart = jsonResult.indexOf("\"", colonIndex);
            int quoteEnd = jsonResult.indexOf("\"", quoteStart + 1);
            
            if (quoteStart != -1 && quoteEnd != -1) {
                return jsonResult.substring(quoteStart + 1, quoteEnd).trim();
            }
        } catch (Exception e) {
            System.err.println("Error parsing Vosk result: " + e.getMessage());
        }
        return "";
    }
    
    /**
     * Recognize speech using Google Cloud Speech API (online)
     */
    private String recognizeSpeechGoogle(byte[] audioData) {
        try {
            ByteString audioBytes = ByteString.copyFrom(audioData);
            
            RecognitionConfig config = RecognitionConfig.newBuilder()
                .setEncoding(RecognitionConfig.AudioEncoding.LINEAR16)
                .setSampleRateHertz(16000)
                .setLanguageCode("en-US")
                .build();
            
            RecognitionAudio audio = RecognitionAudio.newBuilder()
                .setContent(audioBytes)
                .build();
            
            RecognizeResponse response = speechClient.recognize(config, audio);
            List<SpeechRecognitionResult> results = response.getResultsList();
            
            if (!results.isEmpty()) {
                SpeechRecognitionAlternative alternative = results.get(0).getAlternativesList().get(0);
                String transcript = alternative.getTranscript();
                System.out.println("You said: " + transcript);
                return transcript.toLowerCase();
            }
        } catch (Exception e) {
            System.err.println("Error recognizing speech with Google API: " + e.getMessage());
        }
        
        return "";
    }
    
    /**
     * Check if the text contains any wake word
     */
    public boolean containsWakeWord(String text) {
        if (text == null || text.isEmpty()) {
            return false;
        }
        
        String lowerText = text.toLowerCase();
        for (String wakeWord : config.getWakeWords()) {
            if (lowerText.contains(wakeWord.toLowerCase().trim())) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Remove wake word from command text
     */
    public String removeWakeWord(String text) {
        if (text == null) return "";
        
        String result = text.toLowerCase();
        for (String wakeWord : config.getWakeWords()) {
            result = result.replace(wakeWord.toLowerCase().trim(), "").trim();
        }
        return result;
    }
    
    /**
     * Listen once without wake word (push-to-talk mode)
     * @param durationSeconds How long to listen
     * @return Recognized text
     */
    public String listenOnce(int durationSeconds) {
        return listen(durationSeconds);
    }
    
    /**
     * Listen with real-time callback for audio level and transcription
     * @param durationSeconds How long to listen
     * @param callback Callback for audio level updates
     * @return Final recognized text
     */
    public String listenWithCallback(int durationSeconds, AudioLevelCallback callback) {
        try {
            isListening = true;
            byte[] audioData = captureAudioWithCallback(durationSeconds, callback);
            String result = recognizeSpeech(audioData);
            if (callback != null && result != null && !result.isEmpty()) {
                callback.onTranscript(result);
            }
            isListening = false;
            return result;
        } catch (Exception e) {
            isListening = false;
            System.err.println("Error during speech recognition: " + e.getMessage());
            return "";
        }
    }
    
    /**
     * Capture audio with real-time level callback
     */
    private byte[] captureAudioWithCallback(int durationSeconds, AudioLevelCallback callback) throws LineUnavailableException {
        DataLine.Info info = new DataLine.Info(TargetDataLine.class, audioFormat);
        
        if (!AudioSystem.isLineSupported(info)) {
            System.err.println("Audio line not supported");
            return new byte[0];
        }
        
        TargetDataLine targetLine = (TargetDataLine) AudioSystem.getLine(info);
        targetLine.open(audioFormat);
        targetLine.start();
        
        System.out.println("🎤 Listening...");
        
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[4096];
        
        long endTime = System.currentTimeMillis() + (durationSeconds * 1000);
        
        while (System.currentTimeMillis() < endTime) {
            int bytesRead = targetLine.read(buffer, 0, buffer.length);
            out.write(buffer, 0, bytesRead);
            
            // Calculate audio level
            if (callback != null) {
                float level = calculateAudioLevel(buffer, bytesRead);
                currentAudioLevel = level;
                callback.onAudioLevel(level);
            }
        }
        
        targetLine.stop();
        targetLine.close();
        
        System.out.println("✅ Recording complete");
        
        return out.toByteArray();
    }
    
    /**
     * Calculate audio level from buffer (0.0 to 1.0)
     */
    private float calculateAudioLevel(byte[] buffer, int bytesRead) {
        long sum = 0;
        for (int i = 0; i < bytesRead - 1; i += 2) {
            int sample = (buffer[i+1] << 8) | (buffer[i] & 0xFF);
            sum += Math.abs(sample);
        }
        float average = (float) sum / (bytesRead / 2);
        float normalized = Math.min(average / 32768.0f, 1.0f);
        
        // Apply automatic gain if audio is very quiet
        if (normalized < 0.1f && normalized > 0.0f) {
            // Amplify quiet audio by up to 10x
            normalized = Math.min(normalized * 10.0f, 1.0f);
        }
        
        return normalized;
    }
    
    /**
     * Get current audio level (0.0 to 1.0)
     */
    public float getAudioLevel() {
        return currentAudioLevel;
    }
    
    /**
     * Check if currently listening
     */
    public boolean isListening() {
        return isListening;
    }
    
    /**
     * Test microphone availability and functionality
     * @return true if microphone is working
     */
    public boolean testMicrophone() {
        try {
            System.out.println("\n🔍 Testing microphone...");
            
            // Check if audio line is supported
            DataLine.Info info = new DataLine.Info(TargetDataLine.class, audioFormat);
            if (!AudioSystem.isLineSupported(info)) {
                System.err.println("❌ Audio line not supported");
                return false;
            }
            System.out.println("✅ Audio line is supported");
            
            // List available mixers
            Mixer.Info[] mixers = AudioSystem.getMixerInfo();
            System.out.println("\nAvailable audio devices:");
            for (int i = 0; i < mixers.length; i++) {
                System.out.println("  " + (i+1) + ". " + mixers[i].getName());
            }
            
            // Try to open and test the line
            TargetDataLine line = (TargetDataLine) AudioSystem.getLine(info);
            line.open(audioFormat);
            line.start();
            
            System.out.println("\n🎤 Recording test (2 seconds)...");
            System.out.println("Please make some noise...");
            
            byte[] buffer = new byte[4096];
            long totalBytes = 0;
            float maxLevel = 0.0f;
            long endTime = System.currentTimeMillis() + 2000;
            
            while (System.currentTimeMillis() < endTime) {
                int bytesRead = line.read(buffer, 0, buffer.length);
                totalBytes += bytesRead;
                float level = calculateAudioLevel(buffer, bytesRead);
                maxLevel = Math.max(maxLevel, level);
            }
            
            line.stop();
            line.close();
            
            System.out.println("\n📊 Test Results:");
            System.out.println("   Total bytes captured: " + totalBytes);
            System.out.println("   Max audio level: " + String.format("%.2f%%", maxLevel * 100));
            
            if (maxLevel < 0.005f) {
                System.err.println("⚠️  WARNING: Very low audio level. Microphone may be muted or not working.");
                System.err.println("   Try: pactl set-source-volume @DEFAULT_SOURCE@ 150%");
                return false;
            } else if (maxLevel < 0.02f) {
                System.out.println("⚠️  Audio level is low but detectable.");
                System.out.println("   Microphone will work but may need louder speech.");
                System.out.println("   Consider increasing volume: pactl set-source-volume @DEFAULT_SOURCE@ 150%");
                return true; // Allow it to work with low volume
            } else {
                System.out.println("✅ Microphone is working!");
                return true;
            }
            
        } catch (Exception e) {
            System.err.println("❌ Error testing microphone: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Clean up resources
     */
    public void shutdown() {
        isListening = false;
        if (speechClient != null) {
            speechClient.close();
        }
        if (voskModel != null) {
            voskModel.close();
        }
    }
}
