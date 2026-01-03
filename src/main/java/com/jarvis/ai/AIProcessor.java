package com.jarvis.ai;

import com.jarvis.config.Config;
import com.jarvis.utils.NetworkChecker;
import okhttp3.*;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonArray;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * AI Processing with support for online (Gemini/Grok) and offline (Ollama) LLMs
 */
public class AIProcessor {
    private final Config config;
    private final OkHttpClient httpClient;
    private final NetworkChecker networkChecker;
    private final String geminiApiKey;
    private final String grokApiKey;
    private final String grokApiUrl;
    private final String grokModel;
    private final List<String> conversationHistory;
    private final String ollamaUrl;
    private final String ollamaModel;
    
    // Timeout settings (in seconds)
    private static final int CONNECT_TIMEOUT = 15;
    private static final int READ_TIMEOUT_ONLINE = 30;
    private static final int READ_TIMEOUT_OLLAMA = 120; // Ollama can be slow
    
    // Manual mode control
    private boolean manualModeEnabled = false;
    private String manualModeSelection = "auto"; // "gemini", "grok", "ollama", or "auto"
    
    // Online AI selection (round-robin)
    private int onlineAIIndex = 0; // 0 = Gemini, 1 = Grok
    private String currentOnlineAI = "gemini";
    
    private static final String GEMINI_API_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-pro:generateContent";
    private static final String DEFAULT_OLLAMA_URL = "http://localhost:11434/api/generate";
    
    public AIProcessor() {
        this.config = Config.getInstance();
        // Configure HTTP client with sensible timeouts
        this.httpClient = new OkHttpClient.Builder()
            .connectTimeout(CONNECT_TIMEOUT, TimeUnit.SECONDS)
            .readTimeout(READ_TIMEOUT_ONLINE, TimeUnit.SECONDS)
            .writeTimeout(CONNECT_TIMEOUT, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build();
        this.networkChecker = NetworkChecker.getInstance();
        this.geminiApiKey = config.getGeminiApiKey();
        this.grokApiKey = config.getGrokApiKey();
        this.grokApiUrl = config.getGrokApiUrl();
        this.grokModel = config.getGrokModel();
        this.conversationHistory = new ArrayList<>();
        
        this.ollamaUrl = config.getProperty("ai.ollama.url", DEFAULT_OLLAMA_URL);
        this.ollamaModel = config.getProperty("ai.ollama.model", "llama2");
        
        // DEFAULT TO GROK (primary AI) - Priority: Grok → Gemini → Ollama
        setManualMode(true, "grok");
        System.out.println("🎯 AI Mode: GROK (default) - Use GUI buttons to switch to Gemini/Ollama");
    }
    
    /**
     * Process a query using Ollama (offline), Gemini, or Grok (online)
     * Automatically selects mode based on network unless manual mode is enabled
     * @param query User's question or command
     * @return AI-generated response
     */
    public String processQuery(String query) {
        String mode = selectAIMode();
        
        if (mode.equals("ollama")) {
            return processWithOllama(query);
        } else if (mode.equals("grok")) {
            return processWithGrok(query);
        } else {
            // mode is "gemini" or online (round-robin)
            return processWithOnlineAI(query);
        }
    }
    
    /**
     * Process with online AI (Gemini or Grok with round-robin)
     */
    private String processWithOnlineAI(String query) {
        // Try current online AI
        String result = null;
        
        if (currentOnlineAI.equals("gemini")) {
            result = processWithGemini(query);
            // Switch to Grok for next query
            if (grokApiKey != null && !grokApiKey.isEmpty()) {
                currentOnlineAI = "grok";
            }
        } else {
            result = processWithGrok(query);
            // Switch to Gemini for next query
            currentOnlineAI = "gemini";
        }
        
        return result;
    }
    
    /**
     * Select AI mode based on network status or manual override
     */
    private String selectAIMode() {
        // Manual mode takes precedence
        if (manualModeEnabled) {
            return manualModeSelection;
        }
        
        // Auto mode: check network
        if (networkChecker.isOnline() && networkChecker.isFastNetwork()) {
            // Return "online" for round-robin selection
            return currentOnlineAI;
        } else {
            return "ollama";
        }
    }
    
    /**
     * Set manual mode override
     * @param manual true to enable manual mode, false for auto mode
     * @param mode "gemini", "grok", or "ollama" (only used if manual is true)
     */
    public void setManualMode(boolean manual, String mode) {
        this.manualModeEnabled = manual;
        if (manual && (mode.equals("gemini") || mode.equals("grok") || mode.equals("ollama"))) {
            this.manualModeSelection = mode;
            System.out.println("🔧 AI Mode: MANUAL - " + mode.toUpperCase());
        } else if (!manual) {
            this.manualModeSelection = "auto";
            System.out.println("🔄 AI Mode: AUTO - " + getCurrentMode().toUpperCase());
        }
    }
    
    /**
     * Get current AI mode being used
     */
    public String getCurrentMode() {
        return selectAIMode();
    }
    
    /**
     * Check if manual mode is enabled
     */
    public boolean isManualMode() {
        return manualModeEnabled;
    }
    
    /**
     * Get network status
     */
    public String getNetworkStatus() {
        return networkChecker.getNetworkStatus();
    }
    
    /**
     * Process query using local Ollama LLM (offline)
     */
    private String processWithOllama(String query) {
        if (query == null || query.trim().isEmpty()) {
            return "I didn't receive a valid query. Please try again.";
        }
        
        try {
            // Build the request JSON for Ollama
            JsonObject requestJson = new JsonObject();
            
            // Add I.R.I.S personality context
            String enhancedQuery = "You are I.R.I.S (Intelligent Responsive Integrated System), an AI assistant. " +
                "Respond in a helpful, intelligent, and slightly witty manner. Keep responses concise. " +
                "User query: " + query;
            
            requestJson.addProperty("model", ollamaModel);
            requestJson.addProperty("prompt", enhancedQuery);
            requestJson.addProperty("stream", false);
            
            // Create request
            RequestBody body = RequestBody.create(
                requestJson.toString(),
                MediaType.parse("application/json")
            );
            
            Request request = new Request.Builder()
                .url(ollamaUrl)
                .post(body)
                .build();
            
            // Execute request with extended timeout for large models
            OkHttpClient clientWithTimeout = httpClient.newBuilder()
                .readTimeout(READ_TIMEOUT_OLLAMA, TimeUnit.SECONDS)
                .build();
            
            try (Response response = clientWithTimeout.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    int code = response.code();
                    if (code == 404) {
                        return "Ollama model '" + ollamaModel + "' not found. " +
                               "Try: ollama pull " + ollamaModel;
                    } else if (code >= 500) {
                        return "Ollama server error. Please restart Ollama: ollama serve";
                    }
                    return "I'm having trouble connecting to Ollama (error " + code + "). " +
                           "Make sure Ollama is running: ollama serve";
                }
                
                String responseBody = response.body() != null ? response.body().string() : null;
                if (responseBody == null || responseBody.isEmpty()) {
                    return "I received an empty response from Ollama. Please try again.";
                }
                return parseOllamaResponse(responseBody);
            }
            
        } catch (SocketTimeoutException e) {
            System.err.println("Ollama timeout: " + e.getMessage());
            return "The AI is taking too long to respond. The model might be loading. " +
                   "Please try again in a moment.";
        } catch (java.net.ConnectException e) {
            System.err.println("Cannot connect to Ollama: " + e.getMessage());
            return "Cannot connect to Ollama. Please start it with: ollama serve";
        } catch (IOException e) {
            System.err.println("Error calling Ollama: " + e.getMessage());
            return "I'm having trouble with my offline AI system. " +
                   "Please ensure Ollama is installed and running. " +
                   "Install: curl -fsSL https://ollama.com/install.sh | sh";
        }
    }
    
    /**
     * Process query using Google Gemini API (online)
     */
    private String processWithGemini(String query) {
        if (geminiApiKey == null || geminiApiKey.isEmpty()) {
            return "I'm sorry, but I need a Gemini API key to answer that question. " +
                   "Please configure your API key in the .env file, or enable offline mode.";
        }
        
        try {
            // Build the request JSON
            JsonObject requestJson = new JsonObject();
            JsonArray contents = new JsonArray();
            JsonObject content = new JsonObject();
            JsonArray parts = new JsonArray();
            JsonObject part = new JsonObject();
            
            // Add context for I.R.I.S personality
            String enhancedQuery = "You are I.R.I.S (Intelligent Responsive Integrated System), " +
                "an advanced AI assistant for penetration testing and security analysis. " +
                "Respond in a helpful, intelligent, and professional manner. " +
                "User query: " + query;
            
            part.addProperty("text", enhancedQuery);
            parts.add(part);
            content.add("parts", parts);
            contents.add(content);
            requestJson.add("contents", contents);
            
            // Create request
            RequestBody body = RequestBody.create(
                requestJson.toString(),
                MediaType.parse("application/json")
            );
            
            Request request = new Request.Builder()
                .url(GEMINI_API_URL + "?key=" + geminiApiKey)
                .post(body)
                .build();
            
            // Execute request
            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    return "I encountered an error while processing your request. Please check your API key.";
                }
                
                String responseBody = response.body().string();
                return parseGeminiResponse(responseBody);
            }
            
        } catch (IOException e) {
            System.err.println("Error calling Gemini API: " + e.getMessage());
            return "I'm having trouble connecting to my AI systems right now.";
        }
    }
    
    /**
     * Process query using Grok API (online)
     */
    private String processWithGrok(String query) {
        if (grokApiKey == null || grokApiKey.isEmpty()) {
            System.out.println("Grok API key not configured, falling back to Gemini");
            return processWithGemini(query);
        }
        
        try {
            // Build the request JSON (OpenAI-compatible format)
            JsonObject requestJson = new JsonObject();
            JsonArray messages = new JsonArray();
            
            // System message
            JsonObject systemMessage = new JsonObject();
            systemMessage.addProperty("role", "system");
            systemMessage.addProperty("content", "You are I.R.I.S (Intelligent Responsive Integrated System), " +
                "an advanced AI assistant for penetration testing and security analysis. " +
                "Respond in a helpful, intelligent, and professional manner.");
            messages.add(systemMessage);
            
            // User message
            JsonObject userMessage = new JsonObject();
            userMessage.addProperty("role", "user");
            userMessage.addProperty("content", query);
            messages.add(userMessage);
            
            requestJson.add("messages", messages);
            requestJson.addProperty("model", grokModel);
            requestJson.addProperty("stream", false);
            requestJson.addProperty("temperature", 0.7);
            
            // Create request
            RequestBody body = RequestBody.create(
                requestJson.toString(),
                MediaType.parse("application/json")
            );
            
            Request request = new Request.Builder()
                .url(grokApiUrl)
                .addHeader("Authorization", "Bearer " + grokApiKey)
                .addHeader("Content-Type", "application/json")
                .post(body)
                .build();
            
            // Execute request
            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    System.err.println("Grok API error: " + response.code());
                    System.out.println("Falling back to Gemini");
                    return processWithGemini(query);
                }
                
                String responseBody = response.body().string();
                return parseGrokResponse(responseBody);
            }
            
        } catch (IOException e) {
            System.err.println("Error calling Grok API: " + e.getMessage());
            System.out.println("Falling back to Gemini");
            return processWithGemini(query);
        }
    }
    
    /**
     * Parse the Ollama API response
     */
    private String parseOllamaResponse(String jsonResponse) {
        try {
            JsonObject responseObj = JsonParser.parseString(jsonResponse).getAsJsonObject();
            
            if (responseObj.has("response")) {
                return responseObj.get("response").getAsString().trim();
            }
            
            return "I couldn't generate a proper response.";
        } catch (Exception e) {
            System.err.println("Error parsing Ollama response: " + e.getMessage());
            return "I had trouble understanding the response from my AI system.";
        }
    }
    
    /**
     * Parse the Gemini API response
     */
    private String parseGeminiResponse(String jsonResponse) {
        try {
            JsonObject responseObj = JsonParser.parseString(jsonResponse).getAsJsonObject();
            
            if (responseObj.has("candidates")) {
                JsonArray candidates = responseObj.getAsJsonArray("candidates");
                if (candidates.size() > 0) {
                    JsonObject candidate = candidates.get(0).getAsJsonObject();
                    JsonObject content = candidate.getAsJsonObject("content");
                    JsonArray parts = content.getAsJsonArray("parts");
                    if (parts.size() > 0) {
                        JsonObject part = parts.get(0).getAsJsonObject();
                        return part.get("text").getAsString();
                    }
                }
            }
            
            return "I couldn't generate a proper response.";
        } catch (Exception e) {
            System.err.println("Error parsing Gemini response: " + e.getMessage());
            return "I had trouble understanding the response from my AI systems.";
        }
    }
    
    /**
     * Parse the Grok API response (OpenAI-compatible format)
     */
    private String parseGrokResponse(String jsonResponse) {
        try {
            JsonObject responseObj = JsonParser.parseString(jsonResponse).getAsJsonObject();
            
            if (responseObj.has("choices")) {
                JsonArray choices = responseObj.getAsJsonArray("choices");
                if (choices.size() > 0) {
                    JsonObject choice = choices.get(0).getAsJsonObject();
                    JsonObject message = choice.getAsJsonObject("message");
                    if (message.has("content")) {
                        return message.get("content").getAsString();
                    }
                }
            }
            
            return "I couldn't generate a proper response.";
        } catch (Exception e) {
            System.err.println("Error parsing Grok response: " + e.getMessage());
            return "I had trouble understanding the response from Grok.";
        }
    }
    
    /**
     * Add message to conversation history
     */
    public void addToHistory(String message) {
        conversationHistory.add(message);
        // Keep only last 10 messages to avoid token limits
        if (conversationHistory.size() > 10) {
            conversationHistory.remove(0);
        }
    }
    
    /**
     * Clear conversation history
     */
    public void clearHistory() {
        conversationHistory.clear();
    }
    
    /**
     * Check if currently using offline mode (Ollama)
     */
    public boolean isOfflineMode() {
        return getCurrentMode().equals("ollama");
    }
    
    /**
     * Refresh network status (clears cache)
     */
    public void refreshNetworkStatus() {
        networkChecker.refresh();
    }
}
