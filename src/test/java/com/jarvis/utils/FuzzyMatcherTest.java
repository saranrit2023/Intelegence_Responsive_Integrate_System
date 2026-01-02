package com.jarvis.utils;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for FuzzyMatcher class
 */
class FuzzyMatcherTest {
    
    @Test
    void testLevenshteinDistance_exactMatch() {
        assertEquals(0, FuzzyMatcher.levenshteinDistance("hello", "hello"));
    }
    
    @Test
    void testLevenshteinDistance_singleEdit() {
        assertEquals(1, FuzzyMatcher.levenshteinDistance("hello", "hallo"));
    }
    
    @Test
    void testLevenshteinDistance_multipleEdits() {
        assertEquals(3, FuzzyMatcher.levenshteinDistance("kitten", "sitting"));
    }
    
    @Test
    void testLevenshteinDistance_caseInsensitive() {
        assertEquals(0, FuzzyMatcher.levenshteinDistance("Hello", "hello"));
    }
    
    @Test
    void testSimilarity_exactMatch() {
        assertEquals(100.0, FuzzyMatcher.similarity("test", "test"));
    }
    
    @Test
    void testSimilarity_partialMatch() {
        double sim = FuzzyMatcher.similarity("firefox", "firefoks");
        assertTrue(sim > 70, "Should have reasonable similarity for minor typo");
    }
    
    @Test
    void testFuzzyMatch_aboveThreshold() {
        assertTrue(FuzzyMatcher.fuzzyMatch("firefox", "firefoks", 70));
    }
    
    @Test
    void testFuzzyMatch_belowThreshold() {
        assertFalse(FuzzyMatcher.fuzzyMatch("firefox", "chrome", 80));
    }
    
    @Test
    void testFindBestMatch_withMatch() {
        String[] options = {"firefox", "chrome", "safari", "edge"};
        String result = FuzzyMatcher.findBestMatch("firefoks", options, 70);
        assertEquals("firefox", result);
    }
    
    @Test
    void testFindBestMatch_noMatch() {
        String[] options = {"firefox", "chrome", "safari"};
        String result = FuzzyMatcher.findBestMatch("xyz123", options, 70);
        assertNull(result);
    }
    
    @Test
    void testSoundex() {
        String soundex = FuzzyMatcher.soundex("Robert");
        assertNotNull(soundex);
        assertEquals(4, soundex.length(), "Soundex should be 4 characters");
    }
    
    @Test
    void testSoundsLike() {
        assertTrue(FuzzyMatcher.soundsLike("Robert", "Rubert"));
    }
    
    @Test
    void testSmartMatch_exactMatch() {
        assertTrue(FuzzyMatcher.smartMatch("firefox", "firefox"));
    }
    
    @Test
    void testSmartMatch_containsMatch() {
        assertTrue(FuzzyMatcher.smartMatch("open firefox browser", "firefox"));
    }
    
    @Test
    void testSmartMatch_fuzzyMatch() {
        assertTrue(FuzzyMatcher.smartMatch("firefoks", "firefox"));
    }
    
    @Test
    void testFindSmartMatch() {
        String[] options = {"firefox", "chrome", "terminal", "wireshark"};
        assertEquals("firefox", FuzzyMatcher.findSmartMatch("fire fox", options));
    }
    
    @Test
    void testFindSmartMatch_noMatch() {
        String[] options = {"firefox", "chrome"};
        assertNull(FuzzyMatcher.findSmartMatch("xyz", options));
    }
}
