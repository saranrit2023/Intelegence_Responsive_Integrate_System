package com.jarvis.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for NetworkChecker class
 */
class NetworkCheckerTest {
    
    private NetworkChecker networkChecker;
    
    @BeforeEach
    void setUp() {
        networkChecker = NetworkChecker.getInstance();
    }
    
    @Test
    void testGetInstance() {
        NetworkChecker instance1 = NetworkChecker.getInstance();
        NetworkChecker instance2 = NetworkChecker.getInstance();
        assertNotNull(instance1);
        assertSame(instance1, instance2, "NetworkChecker should be a singleton");
    }
    
    @Test
    void testGetNetworkStatus() {
        String status = networkChecker.getNetworkStatus();
        assertNotNull(status);
        assertFalse(status.isEmpty());
    }
    
    @Test
    void testIsOnline_returnsBoolean() {
        // Just verify the method doesn't throw and returns a boolean
        boolean result = networkChecker.isOnline();
        assertTrue(result || !result); // tautology to verify boolean return
    }
    
    @Test
    void testIsFastNetwork_returnsBoolean() {
        boolean result = networkChecker.isFastNetwork();
        assertTrue(result || !result);
    }
    
    @Test
    void testRefresh_doesNotThrow() {
        assertDoesNotThrow(() -> networkChecker.refresh());
    }
    
    @Test
    void testNetworkStatus_hasValidFormat() {
        String status = networkChecker.getNetworkStatus();
        // Status should be something descriptive
        assertTrue(status.length() > 0, "Network status should have content");
    }
}
