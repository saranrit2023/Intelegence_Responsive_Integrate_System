package com.jarvis.commands;

import com.jarvis.config.Config;
import com.jarvis.utils.FuzzyMatcher;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * System-level commands (open applications, control volume, etc.)
 */
public class SystemCommands {
    private final Config config;
    
    // Known applications with common variations
    private static final String[][] APP_MAPPINGS = {
        {"firefox", "fire fox", "mozilla", "fox"},
        {"chrome", "google chrome", "google-chrome"},
        {"brave", "brave browser"},
        {"wireshark", "wire shark", "shark"},
        {"burpsuite", "burp suite", "burp"},
        {"metasploit", "meta sploit", "msfconsole", "msf"},
        {"terminal", "gnome-terminal", "console"},
        {"calculator", "calc", "gnome-calculator"},
        {"nautilus", "files", "file manager", "folders"},
        {"gedit", "text editor", "editor"},
        {"code", "vs code", "visual studio code", "vscode"},
        {"spotify", "music"},
        {"gnome-control-center", "settings", "system settings"},
        {"hydra-gtk", "hydra", "brute force", "bruteforce"},
        {"aircrack-ng", "aircrack", "air crack"},
        {"sqlmap", "sql map"},
        {"john", "john the ripper"},
        {"maltego"},
        {"beef-xss", "beef"},
        {"ettercap"},
        {"armitage"},
        {"ghidra"},
        {"zenmap", "nmap"},
        {"ida", "ida pro"},
        {"gdb", "debugger"}
    };
    
    public SystemCommands() {
        this.config = Config.getInstance();
    }
    
    /**
     * Open an application with fuzzy matching support
     */
    public String openApplication(String appName) {
        try {
            String originalName = appName;
            appName = appName.toLowerCase().trim();
            
            // Clean up common words
            appName = appName.replace("the ", "")
                           .replace(" web", "")
                           .replace(" browser", "")
                           .replace(" application", "")
                           .replace(" app", "")
                           .trim();
            
            // Try fuzzy matching against known apps
            String matchedApp = findMatchingApp(appName);
            
            if (matchedApp != null) {
                String command = getCommandForApp(matchedApp);
                String displayName = getDisplayName(matchedApp);
                
                executeCommand(command);
                return "Opening " + displayName;
            }
            
            // Fallback: Try to open by name directly
            String command = appName.replace(" ", "-");
            if (tryExecuteCommand(command)) {
                return "Opening " + originalName;
            }
            
            // Try without dashes
            command = appName.replace(" ", "").replace("-", "");
            if (tryExecuteCommand(command)) {
                return "Opening " + originalName;
            }
            
            return "I don't know how to open " + originalName + ". Make sure it's installed.";
            
        } catch (Exception e) {
            return "I couldn't open " + appName + ": " + e.getMessage();
        }
    }
    
    /**
     * Find matching app using fuzzy matching
     */
    private String findMatchingApp(String input) {
        // First try exact match in mappings
        for (String[] mapping : APP_MAPPINGS) {
            for (String variant : mapping) {
                if (input.equalsIgnoreCase(variant) || input.contains(variant)) {
                    return mapping[0]; // Return canonical name
                }
            }
        }
        
        // Try fuzzy matching
        for (String[] mapping : APP_MAPPINGS) {
            for (String variant : mapping) {
                if (FuzzyMatcher.smartMatch(input, variant)) {
                    return mapping[0];
                }
            }
        }
        
        return null;
    }
    
    /**
     * Get command for app
     */
    private String getCommandForApp(String app) {
        switch (app) {
            case "firefox": return "firefox";
            case "chrome": return "google-chrome";
            case "brave": return "brave-browser";
            case "wireshark": return "wireshark";
            case "burpsuite": return "burpsuite";
            case "metasploit": return "msfconsole";
            case "terminal": return "gnome-terminal";
            case "calculator": return "gnome-calculator";
            case "nautilus": return "nautilus";
            case "gedit": return "gedit";
            case "code": return "code";
            case "spotify": return "spotify";
            case "gnome-control-center": return "gnome-control-center";
            case "hydra-gtk": return "hydra-gtk";
            case "aircrack-ng": return "aircrack-ng";
            case "sqlmap": return "sqlmap";
            case "john": return "john";
            case "maltego": return "maltego";
            case "beef-xss": return "beef-xss";
            case "ettercap": return "ettercap";
            case "armitage": return "armitage";
            case "ghidra": return "ghidra";
            case "zenmap": return "zenmap";
            case "ida": return "ida";
            case "gdb": return "gdb";
            default: return app;
        }
    }
    
    /**
     * Get display name for app
     */
    private String getDisplayName(String app) {
        switch (app) {
            case "firefox": return "Firefox";
            case "chrome": return "Chrome";
            case "brave": return "Brave";
            case "wireshark": return "Wireshark";
            case "burpsuite": return "Burp Suite";
            case "metasploit": return "Metasploit";
            case "terminal": return "Terminal";
            case "calculator": return "Calculator";
            case "nautilus": return "File Manager";
            case "gedit": return "Text Editor";
            case "code": return "VS Code";
            case "spotify": return "Spotify";
            case "gnome-control-center": return "System Settings";
            case "hydra-gtk": return "Hydra";
            case "aircrack-ng": return "Aircrack-ng";
            case "sqlmap": return "SQLMap";
            case "john": return "John the Ripper";
            case "maltego": return "Maltego";
            case "beef-xss": return "BeEF";
            case "ettercap": return "Ettercap";
            case "armitage": return "Armitage";
            case "ghidra": return "Ghidra";
            case "zenmap": return "Nmap";
            case "ida": return "IDA Pro";
            case "gdb": return "GDB";
            default: return app;
        }
    }
    
    /**
     * Try to execute a command and return true if successful
     */
    private boolean tryExecuteCommand(String command) {
        try {
            // Check if command exists using 'which'
            ProcessBuilder checkBuilder = new ProcessBuilder("/bin/sh", "-c", "which " + command);
            Process checkProcess = checkBuilder.start();
            int exitCode = checkProcess.waitFor();
            
            if (exitCode == 0) {
                executeCommand(command);
                return true;
            }
            return false;
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Control system volume
     */
    public String setVolume(String action) {
        try {
            String command = "";
            String volumeCmd = config.getVolumeCommand();
            
            if (volumeCmd.equals("pactl")) {
                switch (action.toLowerCase()) {
                    case "up":
                    case "increase":
                        command = "pactl set-sink-volume @DEFAULT_SINK@ +10%";
                        break;
                    case "down":
                    case "decrease":
                        command = "pactl set-sink-volume @DEFAULT_SINK@ -10%";
                        break;
                    case "mute":
                        command = "pactl set-sink-mute @DEFAULT_SINK@ toggle";
                        break;
                    default:
                        return "I don't understand that volume command";
                }
            } else {
                // Use amixer as fallback
                switch (action.toLowerCase()) {
                    case "up":
                    case "increase":
                        command = "amixer set Master 10%+";
                        break;
                    case "down":
                    case "decrease":
                        command = "amixer set Master 10%-";
                        break;
                    case "mute":
                        command = "amixer set Master toggle";
                        break;
                    default:
                        return "I don't understand that volume command";
                }
            }
            
            executeCommand(command);
            return "Volume " + action;
            
        } catch (Exception e) {
            return "I couldn't change the volume: " + e.getMessage();
        }
    }
    
    /**
     * Shutdown or restart the system
     */
    public String powerCommand(String action) {
        String message = "";
        
        switch (action.toLowerCase()) {
            case "shutdown":
            case "power off":
                message = "Shutting down the system. Goodbye!";
                break;
            case "restart":
            case "reboot":
                message = "Restarting the system.";
                break;
            case "sleep":
            case "suspend":
                message = "Putting the system to sleep.";
                break;
            default:
                return "I don't understand that power command";
        }
        
        // Note: These commands typically require sudo privileges
        // Uncomment the following lines to actually execute power commands
        // try {
        //     String command = "";
        //     switch (action.toLowerCase()) {
        //         case "shutdown":
        //         case "power off":
        //             command = "shutdown -h now";
        //             break;
        //         case "restart":
        //         case "reboot":
        //             command = "shutdown -r now";
        //             break;
        //         case "sleep":
        //         case "suspend":
        //             command = "systemctl suspend";
        //             break;
        //     }
        //     executeCommand(command);
        // } catch (Exception e) {
        //     return "I couldn't execute that power command: " + e.getMessage();
        // }
        
        return message + " (Note: Power commands require administrator privileges and are currently disabled for safety)";
    }
    
    /**
     * Get system information
     */
    public String getSystemInfo(String infoType) {
        try {
            String command = "";
            
            switch (infoType.toLowerCase()) {
                case "battery":
                    command = "upower -i /org/freedesktop/UPower/devices/battery_BAT0 | grep percentage";
                    break;
                case "disk":
                case "storage":
                    command = "df -h / | tail -1";
                    break;
                case "memory":
                case "ram":
                    command = "free -h | grep Mem";
                    break;
                case "cpu":
                    command = "top -bn1 | grep 'Cpu(s)'";
                    break;
                default:
                    return "I don't know how to get that system information";
            }
            
            String result = executeCommandWithOutput(command);
            return result.isEmpty() ? "Couldn't retrieve system information" : result;
            
        } catch (Exception e) {
            return "Error getting system information: " + e.getMessage();
        }
    }
    
    /**
     * Execute a shell command
     */
    private void executeCommand(String command) throws IOException {
        ProcessBuilder processBuilder = new ProcessBuilder("/bin/sh", "-c", command);
        processBuilder.start();
    }
    
    /**
     * Execute a shell command and return output
     */
    private String executeCommandWithOutput(String command) throws IOException {
        ProcessBuilder processBuilder = new ProcessBuilder("/bin/sh", "-c", command);
        Process process = processBuilder.start();
        
        BufferedReader reader = new BufferedReader(
            new InputStreamReader(process.getInputStream())
        );
        
        StringBuilder output = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            output.append(line).append("\n");
        }
        
        return output.toString().trim();
    }
}
