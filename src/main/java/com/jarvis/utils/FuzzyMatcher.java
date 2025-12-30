package com.jarvis.utils;

/**
 * Fuzzy string matching utility for handling misspellings and phonetic variations
 */
public class FuzzyMatcher {
    
    /**
     * Calculate Levenshtein distance between two strings
     * (minimum number of edits needed to transform one string into another)
     */
    public static int levenshteinDistance(String s1, String s2) {
        s1 = s1.toLowerCase();
        s2 = s2.toLowerCase();
        
        int[][] dp = new int[s1.length() + 1][s2.length() + 1];
        
        for (int i = 0; i <= s1.length(); i++) {
            dp[i][0] = i;
        }
        for (int j = 0; j <= s2.length(); j++) {
            dp[0][j] = j;
        }
        
        for (int i = 1; i <= s1.length(); i++) {
            for (int j = 1; j <= s2.length(); j++) {
                int cost = (s1.charAt(i - 1) == s2.charAt(j - 1)) ? 0 : 1;
                dp[i][j] = Math.min(
                    Math.min(dp[i - 1][j] + 1, dp[i][j - 1] + 1),
                    dp[i - 1][j - 1] + cost
                );
            }
        }
        
        return dp[s1.length()][s2.length()];
    }
    
    /**
     * Calculate similarity percentage between two strings
     */
    public static double similarity(String s1, String s2) {
        int maxLen = Math.max(s1.length(), s2.length());
        if (maxLen == 0) return 1.0;
        
        int distance = levenshteinDistance(s1, s2);
        return (1.0 - (double) distance / maxLen) * 100;
    }
    
    /**
     * Check if two strings are similar enough (fuzzy match)
     * @param s1 First string
     * @param s2 Second string
     * @param threshold Similarity threshold (0-100)
     * @return true if similarity >= threshold
     */
    public static boolean fuzzyMatch(String s1, String s2, double threshold) {
        return similarity(s1, s2) >= threshold;
    }
    
    /**
     * Find best match from a list of options
     * @param input Input string
     * @param options Array of possible matches
     * @param threshold Minimum similarity threshold
     * @return Best matching option or null if none match
     */
    public static String findBestMatch(String input, String[] options, double threshold) {
        String bestMatch = null;
        double bestSimilarity = 0;
        
        for (String option : options) {
            double sim = similarity(input, option);
            if (sim >= threshold && sim > bestSimilarity) {
                bestSimilarity = sim;
                bestMatch = option;
            }
        }
        
        return bestMatch;
    }
    
    /**
     * Phonetic matching using simplified Soundex algorithm
     */
    public static String soundex(String s) {
        s = s.toUpperCase();
        StringBuilder result = new StringBuilder();
        
        // Keep first letter
        result.append(s.charAt(0));
        
        // Encode remaining letters
        String codes = "01230120022455012623010202";
        char prevCode = '0';
        
        for (int i = 1; i < s.length() && result.length() < 4; i++) {
            char c = s.charAt(i);
            if (c >= 'A' && c <= 'Z') {
                char code = codes.charAt(c - 'A');
                if (code != '0' && code != prevCode) {
                    result.append(code);
                    prevCode = code;
                }
            }
        }
        
        // Pad with zeros
        while (result.length() < 4) {
            result.append('0');
        }
        
        return result.toString();
    }
    
    /**
     * Check if two strings sound similar (phonetic match)
     */
    public static boolean soundsLike(String s1, String s2) {
        return soundex(s1).equals(soundex(s2));
    }
    
    /**
     * Smart match: combines fuzzy and phonetic matching
     */
    public static boolean smartMatch(String input, String target) {
        input = input.toLowerCase().trim();
        target = target.toLowerCase().trim();
        
        // Exact match
        if (input.equals(target)) return true;
        
        // Contains match
        if (input.contains(target) || target.contains(input)) return true;
        
        // Fuzzy match (70% similarity)
        if (fuzzyMatch(input, target, 70)) return true;
        
        // Phonetic match
        if (soundsLike(input, target)) return true;
        
        return false;
    }
    
    /**
     * Find best smart match from options
     */
    public static String findSmartMatch(String input, String[] options) {
        // First try exact/contains match
        for (String option : options) {
            if (input.equalsIgnoreCase(option) || 
                input.toLowerCase().contains(option.toLowerCase()) ||
                option.toLowerCase().contains(input.toLowerCase())) {
                return option;
            }
        }
        
        // Try fuzzy match
        String fuzzyMatch = findBestMatch(input, options, 70);
        if (fuzzyMatch != null) return fuzzyMatch;
        
        // Try phonetic match
        for (String option : options) {
            if (soundsLike(input, option)) {
                return option;
            }
        }
        
        return null;
    }
}
