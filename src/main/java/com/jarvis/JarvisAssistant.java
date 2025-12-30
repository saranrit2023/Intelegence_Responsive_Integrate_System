package com.jarvis;

import com.jarvis.commands.CommandHandler;
import com.jarvis.config.Config;
import com.jarvis.speech.SpeechRecognizer;
import com.jarvis.speech.TextToSpeech;

import java.util.Scanner;

/**
 * Main JARVIS Voice Assistant Application
 */
public class JarvisAssistant {
    private final TextToSpeech tts;
    private final SpeechRecognizer speechRecognizer;
    private final CommandHandler commandHandler;
    private final Config config;
    private boolean running;
    
    public JarvisAssistant() {
        this.config = Config.getInstance();
        this.tts = new TextToSpeech();
        this.speechRecognizer = new SpeechRecognizer();
        this.commandHandler = new CommandHandler(tts);
        this.running = true;
    }
    
    /**
     * Start the voice assistant
     */
    public void start() {
        System.out.println("=".repeat(60));
        System.out.println("I.R.I.S Voice Assistant - Initializing...");
        System.out.println("=".repeat(60));
        
        tts.speak("Hello! I am I.R.I.S, your voice assistant. How may I help you?");
        
        System.out.println("\nListening for wake word: 'Jarvis' or 'Hey Jarvis'");
        System.out.println("Type 'text' to switch to text mode, or 'quit' to exit\n");
        
        // Check speech recognition mode
        if (config.isSpeechOfflineMode()) {
            System.out.println("🎤 Speech Recognition: OFFLINE mode (Vosk)");
            System.out.println("✅ No API keys needed - works completely offline!\n");
        } else {
            // Check if Google Cloud credentials are set for online mode
            String credPath = config.getGoogleCredentialsPath();
            if (credPath == null || credPath.isEmpty()) {
                System.out.println("⚠ WARNING: Google Cloud credentials not set.");
                System.out.println("  For online mode, set GOOGLE_APPLICATION_CREDENTIALS in .env");
                System.out.println("  OR enable offline mode in config.properties:");
                System.out.println("  speech.offline.mode=true");
                System.out.println("\n  Switching to text input mode...\n");
                runTextMode();
                return;
            }
            System.out.println("🎤 Speech Recognition: ONLINE mode (Google Cloud)\n");
        }
        
        // Test microphone before starting
        System.out.println("Testing microphone...");
        if (!speechRecognizer.testMicrophone()) {
            System.err.println("\n⚠️  Microphone test failed!");
            System.err.println("  Voice recognition may not work properly.");
            System.err.println("  Switching to text input mode...\n");
            runTextMode();
            return;
        }
        
        System.out.println("\n✅ Ready! Say 'Jarvis' or 'Hey Jarvis' to activate.\n");
        
        // Start main loop
        runVoiceMode();
    }
    
    /**
     * Run in voice recognition mode
     */
    private void runVoiceMode() {
        Scanner scanner = new Scanner(System.in);
        int consecutiveErrors = 0;
        
        while (running) {
            try {
                // Check for text input option
                if (System.in.available() > 0) {
                    String input = scanner.nextLine().trim();
                    if (input.equalsIgnoreCase("text")) {
                        runTextMode();
                        return;
                    } else if (input.equalsIgnoreCase("quit")) {
                        shutdown();
                        return;
                    }
                }
                
                // Listen for wake word
                System.out.println("\n[Listening for wake word...]");
                String speech = speechRecognizer.listen(5);
                
                if (speech != null && !speech.isEmpty()) {
                    System.out.println("Heard: \"" + speech + "\"");
                    
                    if (speechRecognizer.containsWakeWord(speech)) {
                        System.out.println("✅ Wake word detected!");
                        tts.speak("Yes?");
                        System.out.println("\n[Listening for command...]");
                        
                        // Listen for actual command
                        String command = speechRecognizer.listen(5);
                        
                        if (command != null && !command.isEmpty()) {
                            System.out.println("Command: \"" + command + "\"");
                            processCommand(command);
                            consecutiveErrors = 0; // Reset error counter on success
                        } else {
                            System.out.println("No command detected.");
                        }
                    }
                } else {
                    // No speech detected, this is normal
                    System.out.print(".");
                }
                
                // Small delay to prevent excessive CPU usage
                Thread.sleep(500);
                
            } catch (Exception e) {
                consecutiveErrors++;
                System.err.println("\n⚠️  Error in voice mode: " + e.getMessage());
                
                if (consecutiveErrors >= 3) {
                    System.err.println("Too many consecutive errors. Switching to text mode...");
                    runTextMode();
                    return;
                }
                
                // Wait a bit before retrying
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException ie) {
                    // Ignore
                }
            }
        }
        
        scanner.close();
    }
    
    /**
     * Run in text input mode (fallback)
     */
    private void runTextMode() {
        Scanner scanner = new Scanner(System.in);
        
        System.out.println("\n" + "=".repeat(60));
        System.out.println("TEXT INPUT MODE");
        System.out.println("=".repeat(60));
        System.out.println("Type your commands (or 'exit' to quit)\n");
        
        while (running) {
            System.out.print("You: ");
            String input = scanner.nextLine().trim();
            
            if (input.isEmpty()) {
                continue;
            }
            
            processCommand(input);
        }
        
        scanner.close();
    }
    
    /**
     * Process a command
     */
    private void processCommand(String command) {
        System.out.println("Processing: " + command);
        
        String response = commandHandler.processCommand(command);
        
        if (response.equals("exit")) {
            shutdown();
            return;
        }
        
        tts.speak(response);
    }
    
    /**
     * Shutdown the assistant
     */
    private void shutdown() {
        running = false;
        tts.speak("Goodbye! Shutting down I.R.I.S.");
        
        System.out.println("\nShutting down...");
        
        // Cleanup resources
        tts.shutdown();
        speechRecognizer.shutdown();
        
        System.out.println("I.R.I.S has been shut down.");
        System.exit(0);
    }
    
    /**
     * Main entry point
     */
    public static void main(String[] args) {
        try {
            JarvisAssistant jarvis = new JarvisAssistant();
            jarvis.start();
        } catch (Exception e) {
            System.err.println("Fatal error: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}
