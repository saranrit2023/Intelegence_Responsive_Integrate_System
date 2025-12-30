package com.jarvis.utils;

import java.io.IOException;
import java.net.*;
import java.util.concurrent.*;

/**
 * Network connectivity checker with caching and speed estimation
 */
public class NetworkChecker {
    private static NetworkChecker instance;
    private static final long CACHE_DURATION_MS = 30000; // 30 seconds
    
    // Test endpoints
    private static final String[] TEST_HOSTS = {
        "8.8.8.8",           // Google DNS
        "1.1.1.1"            // Cloudflare DNS
    };
    private static final String[] HTTP_ENDPOINTS = {
        "https://www.google.com",
        "https://www.cloudflare.com"
    };
    
    private static final int PING_TIMEOUT_MS = 3000;
    private static final int HTTP_TIMEOUT_MS = 5000;
    private static final int FAST_NETWORK_THRESHOLD_MS = 1000; // < 1s is fast
    
    private Boolean cachedOnlineStatus = null;
    private Boolean cachedFastStatus = null;
    private long lastCheckTime = 0;
    
    private NetworkChecker() {}
    
    public static synchronized NetworkChecker getInstance() {
        if (instance == null) {
            instance = new NetworkChecker();
        }
        return instance;
    }
    
    /**
     * Check if network is available (cached)
     */
    public boolean isOnline() {
        if (isCacheValid()) {
            return cachedOnlineStatus != null ? cachedOnlineStatus : false;
        }
        
        boolean online = checkNetworkConnectivity();
        cachedOnlineStatus = online;
        lastCheckTime = System.currentTimeMillis();
        return online;
    }
    
    /**
     * Check if network is fast enough for API calls (cached)
     */
    public boolean isFastNetwork() {
        if (isCacheValid()) {
            return cachedFastStatus != null ? cachedFastStatus : false;
        }
        
        if (!isOnline()) {
            cachedFastStatus = false;
            return false;
        }
        
        boolean fast = checkNetworkSpeed();
        cachedFastStatus = fast;
        return fast;
    }
    
    /**
     * Force refresh of network status
     */
    public void refresh() {
        cachedOnlineStatus = null;
        cachedFastStatus = null;
        lastCheckTime = 0;
    }
    
    /**
     * Check if cached values are still valid
     */
    private boolean isCacheValid() {
        return (System.currentTimeMillis() - lastCheckTime) < CACHE_DURATION_MS;
    }
    
    /**
     * Check basic network connectivity
     */
    private boolean checkNetworkConnectivity() {
        // Try ICMP ping first (fastest)
        for (String host : TEST_HOSTS) {
            if (pingHost(host)) {
                return true;
            }
        }
        
        // Fallback to HTTP check
        for (String endpoint : HTTP_ENDPOINTS) {
            if (httpCheck(endpoint)) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Ping a host using InetAddress.isReachable
     */
    private boolean pingHost(String host) {
        try {
            InetAddress address = InetAddress.getByName(host);
            return address.isReachable(PING_TIMEOUT_MS);
        } catch (IOException e) {
            return false;
        }
    }
    
    /**
     * Check HTTP connectivity
     */
    private boolean httpCheck(String urlString) {
        try {
            URL url = new URL(urlString);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("HEAD");
            connection.setConnectTimeout(HTTP_TIMEOUT_MS);
            connection.setReadTimeout(HTTP_TIMEOUT_MS);
            connection.connect();
            
            int responseCode = connection.getResponseCode();
            connection.disconnect();
            
            return (responseCode >= 200 && responseCode < 400);
        } catch (IOException e) {
            return false;
        }
    }
    
    /**
     * Check network speed by measuring response time
     */
    private boolean checkNetworkSpeed() {
        long startTime = System.currentTimeMillis();
        
        try {
            URL url = new URL(HTTP_ENDPOINTS[0]);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("HEAD");
            connection.setConnectTimeout(HTTP_TIMEOUT_MS);
            connection.setReadTimeout(HTTP_TIMEOUT_MS);
            connection.connect();
            
            int responseCode = connection.getResponseCode();
            connection.disconnect();
            
            long duration = System.currentTimeMillis() - startTime;
            
            if (responseCode >= 200 && responseCode < 400) {
                return duration < FAST_NETWORK_THRESHOLD_MS;
            }
        } catch (IOException e) {
            // Network error means slow/unavailable
            return false;
        }
        
        return false;
    }
    
    /**
     * Get network status as a string
     */
    public String getNetworkStatus() {
        if (!isOnline()) {
            return "Offline";
        } else if (isFastNetwork()) {
            return "Online (Fast)";
        } else {
            return "Online (Slow)";
        }
    }
    
    /**
     * Get recommended AI mode based on network
     */
    public String getRecommendedAIMode() {
        if (isOnline() && isFastNetwork()) {
            return "gemini";
        } else {
            return "ollama";
        }
    }
    
    /**
     * Test network checker (for debugging)
     */
    public static void main(String[] args) {
        NetworkChecker checker = NetworkChecker.getInstance();
        
        System.out.println("🔍 Testing Network Connectivity...\n");
        
        System.out.println("Status: " + checker.getNetworkStatus());
        System.out.println("Online: " + checker.isOnline());
        System.out.println("Fast Network: " + checker.isFastNetwork());
        System.out.println("Recommended AI Mode: " + checker.getRecommendedAIMode());
        
        System.out.println("\n✅ Network check complete!");
    }
}
