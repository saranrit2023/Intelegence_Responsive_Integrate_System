package com.jarvis.security;

import com.jarvis.ai.AIProcessor;
import com.google.gson.JsonObject;

import java.io.*;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;
import java.util.regex.Pattern;

/**
 * File analyzer with security scanning and AI-powered summarization
 */
public class FileAnalyzer {
    private final AIProcessor aiProcessor;
    
    // Suspicious patterns for different file types
    private static final Pattern[] SUSPICIOUS_PATTERNS = {
        Pattern.compile("eval\\s*\\(", Pattern.CASE_INSENSITIVE),
        Pattern.compile("exec\\s*\\(", Pattern.CASE_INSENSITIVE),
        Pattern.compile("system\\s*\\(", Pattern.CASE_INSENSITIVE),
        Pattern.compile("shell_exec", Pattern.CASE_INSENSITIVE),
        Pattern.compile("base64_decode", Pattern.CASE_INSENSITIVE),
        Pattern.compile("rm\\s+-rf\\s+/", Pattern.CASE_INSENSITIVE),
        Pattern.compile("chmod\\s+777", Pattern.CASE_INSENSITIVE),
        Pattern.compile("wget.*\\|.*sh", Pattern.CASE_INSENSITIVE),
        Pattern.compile("curl.*\\|.*bash", Pattern.CASE_INSENSITIVE),
        Pattern.compile("nc\\s+-e", Pattern.CASE_INSENSITIVE), // Netcat reverse shell
        Pattern.compile("/bin/sh", Pattern.CASE_INSENSITIVE),
        Pattern.compile("password\\s*=", Pattern.CASE_INSENSITIVE),
        Pattern.compile("api[_-]?key\\s*=", Pattern.CASE_INSENSITIVE)
    };
    
    // Known malicious file signatures (magic bytes)
    private static final Map<String, String> MALICIOUS_SIGNATURES = new HashMap<>();
    static {
        MALICIOUS_SIGNATURES.put("4D5A", "PE Executable (Windows)");
        MALICIOUS_SIGNATURES.put("7F454C46", "ELF Executable (Linux)");
    }
    
    public FileAnalyzer() {
        this.aiProcessor = new AIProcessor();
    }
    
    /**
     * Analyze a file for security threats and generate report
     */
    public FileAnalysisReport analyzeFile(String filePath) {
        FileAnalysisReport report = new FileAnalysisReport(filePath);
        
        try {
            File file = new File(filePath);
            
            if (!file.exists()) {
                report.addError("File does not exist");
                return report;
            }
            
            if (!file.canRead()) {
                report.addError("File is not readable");
                return report;
            }
            
            // Basic file information
            report.setFileSize(file.length());
            report.setLastModified(new Date(file.lastModified()));
            report.setReadable(file.canRead());
            report.setWritable(file.canWrite());
            report.setExecutable(file.isFile() && file.canExecute());
            
            // File type detection
            String fileType = detectFileType(file);
            report.setFileType(fileType);
            
            // Calculate hashes
            calculateHashes(file, report);
            
            // Check for ClamAV
            if (isClamAVAvailable()) {
                scanWithClamAV(file, report);
            } else {
                report.addWarning("ClamAV not available - using pattern-based scanning");
                patternBasedScan(file, report);
            }
            
            // Content analysis for text files
            if (isTextFile(file)) {
                analyzeTextContent(file, report);
            }
            
            // AI-powered summarization
            if (report.getThreatLevel() != ThreatLevel.CRITICAL) {
                generateAISummary(file, report);
            }
            
        } catch (Exception e) {
            report.addError("Analysis failed: " + e.getMessage());
        }
        
        return report;
    }
    
    /**
     * Detect file type using file command and magic bytes
     */
    private String detectFileType(File file) {
        try {
            // Try using 'file' command first
            Process process = Runtime.getRuntime().exec(new String[]{"file", "-b", file.getAbsolutePath()});
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String result = reader.readLine();
            reader.close();
            
            if (result != null && !result.isEmpty()) {
                return result;
            }
        } catch (IOException e) {
            // Fallback to extension-based detection
        }
        
        // Fallback: check extension
        String name = file.getName();
        int dotIndex = name.lastIndexOf('.');
        if (dotIndex > 0) {
            return "." + name.substring(dotIndex + 1) + " file";
        }
        
        return "Unknown";
    }
    
    /**
     * Calculate MD5, SHA1, and SHA256 hashes
     */
    private void calculateHashes(File file, FileAnalysisReport report) {
        try {
            byte[] fileBytes = Files.readAllBytes(file.toPath());
            
            // MD5
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            report.setMd5Hash(bytesToHex(md5.digest(fileBytes)));
            
            // SHA1
            MessageDigest sha1 = MessageDigest.getInstance("SHA-1");
            report.setSha1Hash(bytesToHex(sha1.digest(fileBytes)));
            
            // SHA256
            MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
            report.setSha256Hash(bytesToHex(sha256.digest(fileBytes)));
            
        } catch (Exception e) {
            report.addWarning("Could not calculate hashes: " + e.getMessage());
        }
    }
    
    /**
     * Convert byte array to hex string
     */
    private String bytesToHex(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte b : bytes) {
            result.append(String.format("%02x", b));
        }
        return result.toString();
    }
    
    /**
     * Check if ClamAV is available
     */
    private boolean isClamAVAvailable() {
        try {
            Process process = Runtime.getRuntime().exec(new String[]{"which", "clamscan"});
            return process.waitFor() == 0;
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Scan file with ClamAV
     */
    private void scanWithClamAV(File file, FileAnalysisReport report) {
        try {
            Process process = Runtime.getRuntime().exec(new String[]{
                "clamscan", "--no-summary", file.getAbsolutePath()
            });
            
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.contains("FOUND")) {
                    report.setThreatLevel(ThreatLevel.CRITICAL);
                    report.addThreat("ClamAV detected malware: " + line);
                }
            }
            reader.close();
            
            int exitCode = process.waitFor();
            if (exitCode == 0) {
                report.addInfo("ClamAV scan: Clean");
            } else if (exitCode == 1) {
                report.setThreatLevel(ThreatLevel.CRITICAL);
            }
            
        } catch (Exception e) {
            report.addWarning("ClamAV scan failed: " + e.getMessage());
        }
    }
    
    /**
     * Pattern-based malware scanning
     */
    private void patternBasedScan(File file, FileAnalysisReport report) {
        if (!isTextFile(file)) {
            // Check magic bytes for executables
            try {
                byte[] header = new byte[4];
                try (FileInputStream fis = new FileInputStream(file)) {
                    fis.read(header);
                }
                
                String hexHeader = bytesToHex(header).toUpperCase();
                for (Map.Entry<String, String> entry : MALICIOUS_SIGNATURES.entrySet()) {
                    if (hexHeader.startsWith(entry.getKey())) {
                        report.setThreatLevel(ThreatLevel.HIGH);
                        report.addThreat("Executable file detected: " + entry.getValue());
                    }
                }
            } catch (IOException e) {
                // Ignore
            }
        }
    }
    
    /**
     * Check if file is a text file
     */
    private boolean isTextFile(File file) {
        try {
            String type = Files.probeContentType(file.toPath());
            return type != null && type.startsWith("text/");
        } catch (IOException e) {
            // Fallback: check extension
            String name = file.getName().toLowerCase();
            return name.endsWith(".txt") || name.endsWith(".log") || 
                   name.endsWith(".sh") || name.endsWith(".py") || 
                   name.endsWith(".java") || name.endsWith(".js") ||
                   name.endsWith(".php") || name.endsWith(".html") ||
                   name.endsWith(".xml") || name.endsWith(".json");
        }
    }
    
    /**
     * Analyze text file content for suspicious patterns
     */
    private void analyzeTextContent(File file, FileAnalysisReport report) {
        try {
            String content = new String(Files.readAllBytes(file.toPath()));
            
            for (Pattern pattern : SUSPICIOUS_PATTERNS) {
                if (pattern.matcher(content).find()) {
                    report.setThreatLevel(ThreatLevel.MEDIUM);
                    report.addThreat("Suspicious pattern found: " + pattern.pattern());
                }
            }
            
            // Check for hardcoded credentials
            if (content.contains("password") || content.contains("api_key") || content.contains("secret")) {
                report.addWarning("Possible hardcoded credentials detected");
            }
            
        } catch (IOException e) {
            report.addWarning("Could not analyze text content: " + e.getMessage());
        }
    }
    
    /**
     * Generate AI-powered file summary
     */
    private void generateAISummary(File file, FileAnalysisReport report) {
        try {
            if (!isTextFile(file) || file.length() > 100000) {
                // Skip AI summary for large or binary files
                return;
            }
            
            String content = new String(Files.readAllBytes(file.toPath()));
            
            // Truncate if too long
            if (content.length() > 5000) {
                content = content.substring(0, 5000) + "\n... (truncated)";
            }
            
            String prompt = "Analyze this file and provide a brief security assessment (2-3 sentences):\n\n" +
                          "File: " + file.getName() + "\n" +
                          "Type: " + report.getFileType() + "\n" +
                          "Content:\n" + content;
            
            String summary = aiProcessor.processQuery(prompt);
            report.setAiSummary(summary);
            
        } catch (Exception e) {
            report.addWarning("AI summary generation failed: " + e.getMessage());
        }
    }
    
    /**
     * Threat level enum
     */
    public enum ThreatLevel {
        SAFE, LOW, MEDIUM, HIGH, CRITICAL
    }
    
    /**
     * File analysis report
     */
    public static class FileAnalysisReport {
        private final String filePath;
        private long fileSize;
        private Date lastModified;
        private String fileType;
        private boolean readable;
        private boolean writable;
        private boolean executable;
        private String md5Hash;
        private String sha1Hash;
        private String sha256Hash;
        private ThreatLevel threatLevel = ThreatLevel.SAFE;
        private List<String> threats = new ArrayList<>();
        private List<String> warnings = new ArrayList<>();
        private List<String> info = new ArrayList<>();
        private List<String> errors = new ArrayList<>();
        private String aiSummary;
        
        public FileAnalysisReport(String filePath) {
            this.filePath = filePath;
        }
        
        // Getters and setters
        public String getFilePath() { return filePath; }
        public long getFileSize() { return fileSize; }
        public void setFileSize(long fileSize) { this.fileSize = fileSize; }
        public Date getLastModified() { return lastModified; }
        public void setLastModified(Date lastModified) { this.lastModified = lastModified; }
        public String getFileType() { return fileType; }
        public void setFileType(String fileType) { this.fileType = fileType; }
        public boolean isReadable() { return readable; }
        public void setReadable(boolean readable) { this.readable = readable; }
        public boolean isWritable() { return writable; }
        public void setWritable(boolean writable) { this.writable = writable; }
        public boolean isExecutable() { return executable; }
        public void setExecutable(boolean executable) { this.executable = executable; }
        public String getMd5Hash() { return md5Hash; }
        public void setMd5Hash(String md5Hash) { this.md5Hash = md5Hash; }
        public String getSha1Hash() { return sha1Hash; }
        public void setSha1Hash(String sha1Hash) { this.sha1Hash = sha1Hash; }
        public String getSha256Hash() { return sha256Hash; }
        public void setSha256Hash(String sha256Hash) { this.sha256Hash = sha256Hash; }
        public ThreatLevel getThreatLevel() { return threatLevel; }
        public void setThreatLevel(ThreatLevel level) { 
            if (level.ordinal() > this.threatLevel.ordinal()) {
                this.threatLevel = level;
            }
        }
        public List<String> getThreats() { return threats; }
        public void addThreat(String threat) { this.threats.add(threat); }
        public List<String> getWarnings() { return warnings; }
        public void addWarning(String warning) { this.warnings.add(warning); }
        public List<String> getInfo() { return info; }
        public void addInfo(String info) { this.info.add(info); }
        public List<String> getErrors() { return errors; }
        public void addError(String error) { this.errors.add(error); }
        public String getAiSummary() { return aiSummary; }
        public void setAiSummary(String aiSummary) { this.aiSummary = aiSummary; }
        
        /**
         * Generate human-readable report
         */
        public String toFormattedString() {
            StringBuilder sb = new StringBuilder();
            sb.append("═══════════════════════════════════════════════════════\n");
            sb.append("FILE ANALYSIS REPORT\n");
            sb.append("═══════════════════════════════════════════════════════\n\n");
            
            sb.append("File: ").append(filePath).append("\n");
            sb.append("Size: ").append(formatFileSize(fileSize)).append("\n");
            sb.append("Type: ").append(fileType).append("\n");
            sb.append("Modified: ").append(lastModified).append("\n");
            sb.append("Permissions: ");
            sb.append(readable ? "R" : "-");
            sb.append(writable ? "W" : "-");
            sb.append(executable ? "X" : "-");
            sb.append("\n\n");
            
            sb.append("HASHES:\n");
            sb.append("  MD5:    ").append(md5Hash != null ? md5Hash : "N/A").append("\n");
            sb.append("  SHA1:   ").append(sha1Hash != null ? sha1Hash : "N/A").append("\n");
            sb.append("  SHA256: ").append(sha256Hash != null ? sha256Hash : "N/A").append("\n\n");
            
            sb.append("THREAT LEVEL: ").append(getThreatLevelColor()).append(threatLevel).append("\u001B[0m\n\n");
            
            if (!errors.isEmpty()) {
                sb.append("ERRORS:\n");
                for (String error : errors) {
                    sb.append("  ❌ ").append(error).append("\n");
                }
                sb.append("\n");
            }
            
            if (!threats.isEmpty()) {
                sb.append("THREATS:\n");
                for (String threat : threats) {
                    sb.append("  ⚠️  ").append(threat).append("\n");
                }
                sb.append("\n");
            }
            
            if (!warnings.isEmpty()) {
                sb.append("WARNINGS:\n");
                for (String warning : warnings) {
                    sb.append("  ⚡ ").append(warning).append("\n");
                }
                sb.append("\n");
            }
            
            if (!info.isEmpty()) {
                sb.append("INFO:\n");
                for (String i : info) {
                    sb.append("  ℹ️  ").append(i).append("\n");
                }
                sb.append("\n");
            }
            
            if (aiSummary != null && !aiSummary.isEmpty()) {
                sb.append("AI ANALYSIS:\n");
                sb.append("  ").append(aiSummary).append("\n\n");
            }
            
            sb.append("═══════════════════════════════════════════════════════\n");
            
            return sb.toString();
        }
        
        private String getThreatLevelColor() {
            switch (threatLevel) {
                case SAFE: return "\u001B[32m"; // Green
                case LOW: return "\u001B[36m"; // Cyan
                case MEDIUM: return "\u001B[33m"; // Yellow
                case HIGH: return "\u001B[35m"; // Magenta
                case CRITICAL: return "\u001B[31m"; // Red
                default: return "";
            }
        }
        
        private String formatFileSize(long size) {
            if (size < 1024) return size + " B";
            if (size < 1024 * 1024) return String.format("%.2f KB", size / 1024.0);
            if (size < 1024 * 1024 * 1024) return String.format("%.2f MB", size / (1024.0 * 1024));
            return String.format("%.2f GB", size / (1024.0 * 1024 * 1024));
        }
    }
}
