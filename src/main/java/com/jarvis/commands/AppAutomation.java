package com.jarvis.commands;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * Application automation using xdotool for controlling apps
 */
public class AppAutomation {
    
    /**
     * Type text into the active window
     */
    public String typeText(String text) {
        try {
            executeCommand("xdotool type --delay 50 '" + text + "'");
            return "Typing: " + text;
        } catch (Exception e) {
            return "Failed to type text: " + e.getMessage();
        }
    }
    
    /**
     * Press a key or key combination
     */
    public String pressKey(String key) {
        try {
            executeCommand("xdotool key " + key);
            return "Pressed: " + key;
        } catch (Exception e) {
            return "Failed to press key: " + e.getMessage();
        }
    }
    
    /**
     * Execute a terminal command in the active terminal
     */
    public String executeInTerminal(String command) {
        try {
            // Type the command
            executeCommand("xdotool type --delay 50 '" + command + "'");
            Thread.sleep(100);
            // Press Enter
            executeCommand("xdotool key Return");
            return "Executed in terminal: " + command;
        } catch (Exception e) {
            return "Failed to execute in terminal: " + e.getMessage();
        }
    }
    
    /**
     * Navigate to URL in browser
     */
    public String navigateToUrl(String url) {
        try {
            // Ensure URL has protocol
            if (!url.startsWith("http://") && !url.startsWith("https://")) {
                url = "https://" + url;
            }
            
            // Focus browser and navigate
            Thread.sleep(500); // Wait for browser to open
            executeCommand("xdotool key ctrl+l"); // Focus address bar
            Thread.sleep(200);
            executeCommand("xdotool type --delay 50 '" + url + "'");
            Thread.sleep(200);
            executeCommand("xdotool key Return");
            
            return "Navigating to: " + url;
        } catch (Exception e) {
            return "Failed to navigate: " + e.getMessage();
        }
    }
    
    /**
     * Click at specific coordinates
     */
    public String clickAt(int x, int y) {
        try {
            executeCommand("xdotool mousemove " + x + " " + y + " click 1");
            return "Clicked at position: " + x + ", " + y;
        } catch (Exception e) {
            return "Failed to click: " + e.getMessage();
        }
    }
    
    /**
     * Search in browser
     */
    public String searchInBrowser(String query) {
        try {
            Thread.sleep(500);
            executeCommand("xdotool key ctrl+l"); // Focus address bar
            Thread.sleep(200);
            executeCommand("xdotool type --delay 50 '" + query + "'");
            Thread.sleep(200);
            executeCommand("xdotool key Return");
            
            return "Searching for: " + query;
        } catch (Exception e) {
            return "Failed to search: " + e.getMessage();
        }
    }
    
    /**
     * Switch to a window by name
     */
    public String switchToWindow(String windowName) {
        try {
            executeCommand("xdotool search --name '" + windowName + "' windowactivate");
            return "Switched to: " + windowName;
        } catch (Exception e) {
            return "Failed to switch window: " + e.getMessage();
        }
    }
    
    /**
     * Minimize current window
     */
    public String minimizeWindow() {
        try {
            executeCommand("xdotool getactivewindow windowminimize");
            return "Window minimized";
        } catch (Exception e) {
            return "Failed to minimize: " + e.getMessage();
        }
    }
    
    /**
     * Maximize current window
     */
    public String maximizeWindow() {
        try {
            executeCommand("xdotool getactivewindow windowmaximize");
            return "Window maximized";
        } catch (Exception e) {
            return "Failed to maximize: " + e.getMessage();
        }
    }
    
    /**
     * Close current window
     */
    public String closeWindow() {
        try {
            executeCommand("xdotool key alt+F4");
            return "Window closed";
        } catch (Exception e) {
            return "Failed to close window: " + e.getMessage();
        }
    }
    
    /**
     * Take a screenshot
     */
    public String takeScreenshot(String filename) {
        try {
            if (filename == null || filename.isEmpty()) {
                filename = "screenshot_" + System.currentTimeMillis() + ".png";
            }
            executeCommand("scrot '" + filename + "'");
            return "Screenshot saved as: " + filename;
        } catch (Exception e) {
            return "Failed to take screenshot: " + e.getMessage();
        }
    }
    
    /**
     * Execute a shell command
     */
    private void executeCommand(String command) throws IOException, InterruptedException {
        ProcessBuilder processBuilder = new ProcessBuilder("/bin/sh", "-c", command);
        Process process = processBuilder.start();
        process.waitFor();
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
