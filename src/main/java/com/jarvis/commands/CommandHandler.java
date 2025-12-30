package com.jarvis.commands;

import com.jarvis.ai.AIProcessor;
import com.jarvis.services.WeatherService;
import com.jarvis.speech.TextToSpeech;
import com.jarvis.security.FileAnalyzer;
import com.jarvis.security.LinkChecker;
import com.jarvis.security.PentestingTools;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Main command handler that routes commands to appropriate handlers
 */
public class CommandHandler {
    private final SystemCommands systemCommands;
    private final WebCommands webCommands;
    private final WeatherService weatherService;
    private final AIProcessor aiProcessor;
    private final AppAutomation appAutomation;
    private final FileAnalyzer fileAnalyzer;
    private final LinkChecker linkChecker;
    private final PentestingTools pentestingTools;
    private final TextToSpeech tts;
    
    public CommandHandler(TextToSpeech tts) {
        this.systemCommands = new SystemCommands();
        this.webCommands = new WebCommands();
        this.weatherService = new WeatherService();
        this.aiProcessor = new AIProcessor();
        this.appAutomation = new AppAutomation();
        this.fileAnalyzer = new FileAnalyzer();
        this.linkChecker = new LinkChecker();
        this.pentestingTools = new PentestingTools();
        this.tts = tts;
    }
    
    /**
     * Get the AI processor instance (for GUI access)
     */
    public AIProcessor getAIProcessor() {
        return aiProcessor;
    }
    
    /**
     * Process a voice command and return response
     */
    public String processCommand(String command) {
        if (command == null || command.trim().isEmpty()) {
            return "I didn't catch that. Could you please repeat?";
        }
        
        command = command.toLowerCase().trim();
        
        // Check for complex multi-step commands using LLM
        if (isComplexCommand(command)) {
            return handleComplexCommand(command);
        }
        
        // Time and date commands
        if (command.contains("time")) {
            return getTime();
        }
        if (command.contains("date")) {
            return getDate();
        }
        
        // System commands
        if (command.contains("open")) {
            return handleOpenCommand(command);
        }
        if (command.contains("volume")) {
            return handleVolumeCommand(command);
        }
        if (command.contains("shutdown") || command.contains("restart") || 
            command.contains("sleep")) {
            return handlePowerCommand(command);
        }
        
        // Web commands
        if (command.contains("search google") || command.contains("google")) {
            return handleGoogleSearch(command);
        }
        if (command.contains("youtube") || command.contains("play")) {
            return handleYouTube(command);
        }
        if (command.contains("wikipedia")) {
            return handleWikipedia(command);
        }
        
        // Weather
        if (command.contains("weather")) {
            return handleWeather(command);
        }
        
        // App automation commands
        if (command.contains("go to") || command.contains("navigate to")) {
            return handleNavigation(command);
        }
        if (command.contains("type")) {
            return handleTyping(command);
        }
        if (command.contains("press")) {
            return handleKeyPress(command);
        }
        if (command.contains("run") && command.contains("terminal")) {
            return handleTerminalCommand(command);
        }
        
        // LLM-powered terminal command generation for hacking/security queries
        if (containsSecurityKeywords(command)) {
            return handleSecurityQuery(command);
        }
        
        if (command.contains("minimize")) {
            return appAutomation.minimizeWindow();
        }
        if (command.contains("maximize")) {
            return appAutomation.maximizeWindow();
        }
        if (command.contains("close window") || command.contains("close this")) {
            return appAutomation.closeWindow();
        }
        if (command.contains("screenshot") || command.contains("take a picture")) {
            return appAutomation.takeScreenshot(null);
        }
        
        // File analysis commands
        if (command.contains("analyze file") || command.contains("scan file")) {
            return handleFileAnalysis(command);
        }
        if (command.contains("check link") || command.contains("check url")) {
            return handleLinkCheck(command);
        }
        
        // Pentesting commands
        if (command.contains("scan network") || command.contains("nmap scan") || 
            command.contains("check open ports") || command.contains("check ports")) {
            return handleNmapScan(command);
        }
        if (command.contains("create payload") || command.contains("generate payload")) {
            return handlePayloadGeneration(command);
        }
        if (command.contains("crack password")) {
            return handlePasswordCrack(command);
        }
        if (command.contains("capture packets") || command.contains("unwanted packets") ||
            command.contains("packet analysis") || command.contains("wire shark")) {
            return handlePacketCapture(command);
        }
        if (command.contains("enumerate directories") || command.contains("directory scan")) {
            return handleDirectoryEnum(command);
        }
        if (command.contains("test sql injection") || command.contains("sqlmap")) {
            return handleSQLInjection(command);
        }
        if (command.contains("brute force")) {
            return handleBruteForce(command);
        }
        
        // Exit command
        if (command.contains("exit") || command.contains("quit") || 
            command.contains("goodbye") || command.contains("bye")) {
            return "exit";
        }
        
        // Default: Use AI for general queries
        return aiProcessor.processQuery(command);
    }
    
    private String getTime() {
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("h:mm a");
        return "The time is " + now.format(formatter);
    }
    
    private String getDate() {
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy");
        return "Today is " + now.format(formatter);
    }
    
    private String handleOpenCommand(String command) {
        String app = command.replace("open", "").trim();
        return systemCommands.openApplication(app);
    }
    
    private String handleVolumeCommand(String command) {
        if (command.contains("up") || command.contains("increase")) {
            return systemCommands.setVolume("up");
        } else if (command.contains("down") || command.contains("decrease")) {
            return systemCommands.setVolume("down");
        } else if (command.contains("mute")) {
            return systemCommands.setVolume("mute");
        }
        return "I didn't understand that volume command";
    }
    
    private String handlePowerCommand(String command) {
        if (command.contains("shutdown")) {
            return systemCommands.powerCommand("shutdown");
        } else if (command.contains("restart") || command.contains("reboot")) {
            return systemCommands.powerCommand("restart");
        } else if (command.contains("sleep") || command.contains("suspend")) {
            return systemCommands.powerCommand("sleep");
        }
        return "I didn't understand that power command";
    }
    
    private String handleGoogleSearch(String command) {
        String query = command.replace("search google for", "")
                             .replace("google", "")
                             .replace("search for", "")
                             .trim();
        return webCommands.searchGoogle(query);
    }
    
    private String handleYouTube(String command) {
        String query = command.replace("youtube", "")
                             .replace("play", "")
                             .replace("on youtube", "")
                             .trim();
        return webCommands.playYouTube(query);
    }
    
    private String handleWikipedia(String command) {
        String query = command.replace("wikipedia", "")
                             .replace("search wikipedia for", "")
                             .replace("on wikipedia", "")
                             .trim();
        return webCommands.searchWikipedia(query);
    }
    
    private String handleWeather(String command) {
        String city = command.replace("weather", "")
                            .replace("in", "")
                            .replace("what's the", "")
                            .replace("what is the", "")
                            .trim();
        
        if (city.isEmpty()) {
            return weatherService.getCurrentWeather();
        } else {
            return weatherService.getWeather(city);
        }
    }
    
    private String handleNavigation(String command) {
        String url = command.replace("go to", "")
                           .replace("navigate to", "")
                           .replace("open", "")
                           .trim();
        return appAutomation.navigateToUrl(url);
    }
    
    private String handleTyping(String command) {
        String text = command.replace("type", "")
                            .trim();
        return appAutomation.typeText(text);
    }
    
    private String handleKeyPress(String command) {
        String key = command.replace("press", "")
                           .replace("key", "")
                           .trim();
        
        // Map common key names
        if (key.contains("enter") || key.contains("return")) {
            key = "Return";
        } else if (key.contains("escape") || key.contains("esc")) {
            key = "Escape";
        } else if (key.contains("tab")) {
            key = "Tab";
        } else if (key.contains("space")) {
            key = "space";
        } else if (key.contains("backspace")) {
            key = "BackSpace";
        } else if (key.contains("delete")) {
            key = "Delete";
        }
        
        return appAutomation.pressKey(key);
    }
    
    private String handleTerminalCommand(String command) {
        String cmd = command.replace("run", "")
                           .replace("in terminal", "")
                           .replace("terminal", "")
                           .trim();
        return appAutomation.executeInTerminal(cmd);
    }
    
    /**
     * Handle file analysis command
     */
    private String handleFileAnalysis(String command) {
        String filePath = command.replace("analyze file", "")
                                .replace("scan file", "")
                                .trim();
        
        if (filePath.isEmpty()) {
            return "Please specify a file path. Example: analyze file /path/to/file";
        }
        
        System.out.println("\n🔍 Analyzing file: " + filePath);
        FileAnalyzer.FileAnalysisReport report = fileAnalyzer.analyzeFile(filePath);
        
        // Print report to console
        System.out.println(report.toFormattedString());
        
        // Return summary for GUI
        String summary = "File analysis complete. Threat level: " + report.getThreatLevel();
        if (!report.getThreats().isEmpty()) {
            summary += ". Found " + report.getThreats().size() + " threat(s).";
        }
        return summary + " Check console for full report.";
    }
    
    /**
     * Handle link safety check command
     */
    private String handleLinkCheck(String command) {
        String url = command.replace("check link", "")
                           .replace("check url", "")
                           .trim();
        
        if (url.isEmpty()) {
            return "Please specify a URL. Example: check link https://example.com";
        }
        
        // Add https:// if no protocol specified
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            url = "https://" + url;
        }
        
        System.out.println("\n🔍 Checking link: " + url);
        LinkChecker.LinkAnalysisReport report = linkChecker.checkLink(url);
        
        // Print report to console
        System.out.println(report.toFormattedString());
        
        // Return summary for GUI
        String summary = "Link check complete. Threat level: " + report.getThreatLevel();
        if (!report.getThreats().isEmpty()) {
            summary += ". Found " + report.getThreats().size() + " threat(s).";
        }
        return summary + " Check console for full report.";
    }
    
    /**
     * Check if command contains security/hacking keywords
     */
    private boolean containsSecurityKeywords(String command) {
        String[] keywords = {
            "hack", "crack", "exploit", "penetration", "pentest",
            "wifi password", "network scan", "port scan", "vulnerability",
            "sql injection", "xss", "brute force", "password crack",
            "metasploit", "nmap", "wireshark", "burp", "hydra",
            "aircrack", "sqlmap", "john", "ettercap"
        };
        
        for (String keyword : keywords) {
            if (command.contains(keyword)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Handle security/hacking queries using LLM to generate terminal commands
     */
    private String handleSecurityQuery(String command) {
        // Create a specialized prompt for terminal command generation
        String prompt = "You are a Kali Linux security expert. The user asked: '" + command + "'. " +
                       "Provide the terminal command(s) to accomplish this task with brief explanations. " +
                       "Format: Command followed by brief explanation. " +
                       "Keep it concise and practical for Kali Linux.";
        
        String response = aiProcessor.processQuery(prompt);
        
        // Return formatted response for GUI display
        return "🔐 SECURITY COMMANDS:\n\n" + response;
    }
    
    /**
     * Check if command is complex (contains multiple actions)
     */
    private boolean isComplexCommand(String command) {
        // Check for patterns indicating multiple steps
        return (command.contains(" in ") && command.contains("open")) ||
               (command.contains(" on ") && command.contains("open")) ||
               (command.contains(" and ")) ||
               (command.contains(" then "));
    }
    
    /**
     * Handle complex commands using LLM to break them down and execute automatically
     */
    private String handleComplexCommand(String command) {
        // Ask LLM to break down the command
        String prompt = "You are I.R.I.S, an AI assistant. Break down this command into simple steps: '" + command + "'. " +
                       "Provide a numbered list of actions. " +
                       "Format each action as a simple command. " +
                       "Example for 'open whatsapp web in firefox':\n" +
                       "1. Open Firefox browser\n" +
                       "2. Navigate to web.whatsapp.com\n" +
                       "Keep it concise and actionable.";
        
        String response = aiProcessor.processQuery(prompt);
        
        StringBuilder result = new StringBuilder();
        result.append("📋 PLANNED ACTIONS:\n\n");
        result.append(response);
        result.append("\n\n⚡ Executing actions...\n");
        
        try {
            // Auto-execute each step
            String[] lines = response.split("\n");
            for (String line : lines) {
                if (!line.trim().isEmpty()) {
                    // Remove numbering
                    String action = line.replaceAll("^\\d+\\.\\s*", "").trim();
                    if (!action.isEmpty() && !action.startsWith("#")) {
                        // Execute the action
                        String actionResult = executeAction(action);
                        result.append("\n✓ ").append(actionResult);
                        
                        // Wait between actions
                        Thread.sleep(1500);
                    }
                }
            }
            result.append("\n\n✅ All actions completed!");
        } catch (Exception e) {
            result.append("\n\n❌ Error: ").append(e.getMessage());
        }
        
        return result.toString();
    }
    
    /**
     * Execute a single action
     */
    private String executeAction(String action) {
        action = action.toLowerCase().trim();
        
        // Handle different action types
        if (action.startsWith("open ")) {
            String app = action.replace("open ", "");
            return systemCommands.openApplication(app);
        }
        else if (action.startsWith("navigate to ") || action.startsWith("go to ")) {
            String url = action.replace("navigate to ", "").replace("go to ", "");
            try {
                Thread.sleep(1500); // Wait for browser
                return appAutomation.navigateToUrl(url);
            } catch (Exception e) {
                return "Navigation failed: " + e.getMessage();
            }
        }
        else if (action.startsWith("type ")) {
            String text = action.replace("type ", "");
            return appAutomation.typeText(text);
        }
        else if (action.startsWith("press ")) {
            String key = action.replace("press ", "");
            return appAutomation.pressKey(key);
        }
        else if (action.startsWith("search for ")) {
            String query = action.replace("search for ", "");
            return appAutomation.searchInBrowser(query);
        }
        else if (action.contains("wait")) {
            try {
                Thread.sleep(2000);
                return "Waited";
            } catch (Exception e) {
                return "Wait failed";
            }
        }
        else {
            // Try processing as a regular command
            return processCommand(action);
        }
    }
    /**
     * Handle nmap scan command
     */
    private String handleNmapScan(String command) {
        String target = command.replace("scan network", "")
                              .replace("nmap scan", "")
                              .trim();
        
        if (target.isEmpty()) {
            return "Please specify a target. Example: scan network 192.168.1.0/24";
        }
        
        // Determine scan type
        if (command.contains("full")) {
            return pentestingTools.nmapFullScan(target);
        } else if (command.contains("os")) {
            return pentestingTools.nmapOSDetection(target);
        } else {
            return pentestingTools.nmapQuickScan(target);
        }
    }
    
    /**
     * Handle payload generation command
     */
    private String handlePayloadGeneration(String command) {
        // Parse: "create payload windows reverse shell 192.168.1.10 4444"
        String[] parts = command.split("\\s+");
        
        String platform = "windows";
        String type = "reverse shell";
        String lhost = "127.0.0.1";
        String lport = "4444";
        
        // Extract platform
        if (command.contains("windows")) {
            platform = "windows";
        } else if (command.contains("linux")) {
            platform = "linux";
        } else if (command.contains("android")) {
            platform = "android";
        }
        
        // Extract type
        if (command.contains("reverse")) {
            type = "reverse";
        } else if (command.contains("bind")) {
            type = "bind";
        }
        
        // Extract IP and port (last two numbers in command)
        for (int i = parts.length - 1; i >= 0; i--) {
            if (parts[i].matches("\\d+") && lport.equals("4444")) {
                lport = parts[i];
            } else if (parts[i].matches("\\d+\\.\\d+\\.\\d+\\.\\d+")) {
                lhost = parts[i];
            }
        }
        
        return pentestingTools.generatePayload(platform, type, lhost, lport);
    }
    
    /**
     * Handle password cracking command
     */
    private String handlePasswordCrack(String command) {
        // Parse: "crack password hashes.txt with rockyou"
        String[] parts = command.split("\\s+");
        
        String hashFile = null;
        String wordlist = null;
        
        // Find hash file
        for (int i = 0; i < parts.length; i++) {
            if (parts[i].contains(".txt") || parts[i].contains("/")) {
                hashFile = parts[i];
            }
            if (parts[i].equals("with") && i + 1 < parts.length) {
                wordlist = "/usr/share/wordlists/" + parts[i + 1] + ".txt";
            }
        }
        
        if (hashFile == null) {
            return "Please specify a hash file. Example: crack password /tmp/hashes.txt with rockyou";
        }
        
        return pentestingTools.crackPassword(hashFile, wordlist);
    }
    
    /**
     * Handle packet capture command
     */
    private String handlePacketCapture(String command) {
        // Parse: "capture packets on eth0 for 60 seconds"
        // User specific request: "check for unwanted packets" -> open Wireshark
        if (command.contains("open") || command.contains("gui") || 
            command.contains("unwanted packets") || command.contains("wire shark")) {
            return systemCommands.openApplication("wireshark");
        }
        
        String[] parts = command.split("\\s+");
        
        String iface = "eth0";
        int duration = 30;
        
        for (int i = 0; i < parts.length; i++) {
            if (parts[i].equals("on") && i + 1 < parts.length) {
                iface = parts[i + 1];
            }
            if (parts[i].equals("for") && i + 1 < parts.length) {
                try {
                    duration = Integer.parseInt(parts[i + 1]);
                } catch (NumberFormatException e) {
                    // Keep default
                }
            }
        }
        
        return pentestingTools.capturePackets(iface, duration);
    }
    
    /**
     * Handle directory enumeration command
     */
    private String handleDirectoryEnum(String command) {
        // Parse: "enumerate directories on example.com"
        String url = command.replace("enumerate directories on", "")
                           .replace("directory scan", "")
                           .trim();
        
        if (url.isEmpty()) {
            return "Please specify a URL. Example: enumerate directories on https://example.com";
        }
        
        if (!url.startsWith("http")) {
            url = "https://" + url;
        }
        
        return pentestingTools.dirBuster(url, null);
    }
    
    /**
     * Handle SQL injection testing command
     */
    private String handleSQLInjection(String command) {
        String url = command.replace("test sql injection on", "")
                           .replace("sqlmap", "")
                           .trim();
        
        if (url.isEmpty()) {
            return "Please specify a URL. Example: test sql injection on https://example.com/login?id=1";
        }
        
        if (!url.startsWith("http")) {
            url = "https://" + url;
        }
        
        return pentestingTools.sqlmap(url);
    }
    
    /**
     * Handle brute force command
     */
    private String handleBruteForce(String command) {
        // Parse: "brute force ssh on 192.168.1.10 with wordlist"
        String[] parts = command.split("\\s+");
        
        String service = "ssh";
        String target = "127.0.0.1";
        String wordlist = null;
        
        for (int i = 0; i < parts.length; i++) {
            if (parts[i].equals("force") && i + 1 < parts.length) {
                service = parts[i + 1];
            }
            if (parts[i].equals("on") && i + 1 < parts.length) {
                target = parts[i + 1];
            }
            if (parts[i].equals("with") && i + 1 < parts.length) {
                wordlist = "/usr/share/wordlists/" + parts[i + 1] + ".txt";
            }
        }
        
        return pentestingTools.hydra(target, service, wordlist);
    }
}
