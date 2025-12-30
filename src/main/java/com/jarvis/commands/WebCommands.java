package com.jarvis.commands;

import com.jarvis.config.Config;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * Web-based commands (search, YouTube, Wikipedia, etc.)
 */
public class WebCommands {
    private final Config config;
    
    public WebCommands() {
        this.config = Config.getInstance();
    }
    
    /**
     * Search Google
     */
    public String searchGoogle(String query) {
        try {
            String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8);
            String url = "https://www.google.com/search?q=" + encodedQuery;
            openUrl(url);
            return "Searching Google for " + query;
        } catch (Exception e) {
            return "I couldn't perform the Google search: " + e.getMessage();
        }
    }
    
    /**
     * Play YouTube video
     */
    public String playYouTube(String query) {
        try {
            String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8);
            String url = "https://www.youtube.com/results?search_query=" + encodedQuery;
            openUrl(url);
            return "Searching YouTube for " + query;
        } catch (Exception e) {
            return "I couldn't open YouTube: " + e.getMessage();
        }
    }
    
    /**
     * Search Wikipedia
     */
    public String searchWikipedia(String query) {
        try {
            String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8);
            String url = "https://en.wikipedia.org/wiki/" + encodedQuery.replace("+", "_");
            openUrl(url);
            return "Opening Wikipedia article for " + query;
        } catch (Exception e) {
            return "I couldn't open Wikipedia: " + e.getMessage();
        }
    }
    
    /**
     * Open a specific website
     */
    public String openWebsite(String url) {
        try {
            if (!url.startsWith("http://") && !url.startsWith("https://")) {
                url = "https://" + url;
            }
            openUrl(url);
            return "Opening " + url;
        } catch (Exception e) {
            return "I couldn't open that website: " + e.getMessage();
        }
    }
    
    /**
     * Open URL in default browser
     */
    private void openUrl(String url) throws IOException {
        String browser = config.getBrowserCommand();
        ProcessBuilder processBuilder = new ProcessBuilder(browser, url);
        processBuilder.start();
    }
}
