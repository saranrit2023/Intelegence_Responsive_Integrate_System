package com.jarvis.security;

import java.io.*;
import java.net.*;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import javax.net.ssl.*;
import java.util.*;
import java.util.regex.Pattern;

/**
 * URL safety checker with phishing detection and SSL validation
 */
public class LinkChecker {
    
    // Known malicious TLDs and patterns
    private static final Set<String> SUSPICIOUS_TLDS = new HashSet<>(Arrays.asList(
        ".tk", ".ml", ".ga", ".cf", ".gq", ".xyz", ".top", ".work", ".click"
    ));
    
    // Phishing patterns
    private static final Pattern[] PHISHING_PATTERNS = {
        Pattern.compile("paypal.*verify", Pattern.CASE_INSENSITIVE),
        Pattern.compile("amazon.*account.*suspend", Pattern.CASE_INSENSITIVE),
        Pattern.compile("bank.*urgent", Pattern.CASE_INSENSITIVE),
        Pattern.compile("password.*expire", Pattern.CASE_INSENSITIVE),
        Pattern.compile("click.*here.*now", Pattern.CASE_INSENSITIVE),
        Pattern.compile("verify.*identity", Pattern.CASE_INSENSITIVE),
        Pattern.compile("suspended.*account", Pattern.CASE_INSENSITIVE)
    };
    
    /**
     * Check if a URL is safe
     */
    public LinkAnalysisReport checkLink(String urlString) {
        LinkAnalysisReport report = new LinkAnalysisReport(urlString);
        
        try {
            URL url = new URL(urlString);
            
            // Basic URL analysis
            analyzeURL(url, report);
            
            // Check SSL certificate if HTTPS
            if (url.getProtocol().equalsIgnoreCase("https")) {
                checkSSLCertificate(url, report);
            } else {
                report.addWarning("Not using HTTPS - connection is not encrypted");
            }
            
            // Check for redirects
            checkRedirects(url, report);
            
            // Phishing detection
            detectPhishing(urlString, report);
            
        } catch (MalformedURLException e) {
            report.setThreatLevel(ThreatLevel.HIGH);
            report.addThreat("Invalid URL format");
        } catch (Exception e) {
            report.addWarning("Analysis failed: " + e.getMessage());
        }
        
        return report;
    }
    
    /**
     * Analyze URL structure
     */
    private void analyzeURL(URL url, LinkAnalysisReport report) {
        String host = url.getHost();
        
        // Check for IP address instead of domain
        if (host.matches("\\d+\\.\\d+\\.\\d+\\.\\d+")) {
            report.setThreatLevel(ThreatLevel.MEDIUM);
            report.addThreat("URL uses IP address instead of domain name");
        }
        
        // Check for suspicious TLD
        for (String tld : SUSPICIOUS_TLDS) {
            if (host.endsWith(tld)) {
                report.setThreatLevel(ThreatLevel.MEDIUM);
                report.addWarning("Suspicious top-level domain: " + tld);
            }
        }
        
        // Check for excessive subdomains
        String[] parts = host.split("\\.");
        if (parts.length > 4) {
            report.setThreatLevel(ThreatLevel.LOW);
            report.addWarning("URL has many subdomains (" + parts.length + ")");
        }
        
        // Check for @ symbol (credential phishing)
        if (url.toString().contains("@")) {
            report.setThreatLevel(ThreatLevel.HIGH);
            report.addThreat("URL contains @ symbol - possible credential phishing");
        }
        
        report.setDomain(host);
        report.setProtocol(url.getProtocol());
    }
    
    /**
     * Check SSL certificate validity
     */
    private void checkSSLCertificate(URL url, LinkAnalysisReport report) {
        try {
            HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);
            connection.connect();
            
            Certificate[] certs = connection.getServerCertificates();
            if (certs.length > 0 && certs[0] instanceof X509Certificate) {
                X509Certificate cert = (X509Certificate) certs[0];
                
                // Check expiration
                try {
                    cert.checkValidity();
                    report.addInfo("SSL certificate is valid");
                    report.setSslValid(true);
                } catch (Exception e) {
                    report.setThreatLevel(ThreatLevel.HIGH);
                    report.addThreat("SSL certificate is invalid or expired");
                    report.setSslValid(false);
                }
                
                // Get certificate info
                report.setCertificateIssuer(cert.getIssuerDN().getName());
            }
            
            connection.disconnect();
            
        } catch (SSLException e) {
            report.setThreatLevel(ThreatLevel.HIGH);
            report.addThreat("SSL certificate error: " + e.getMessage());
            report.setSslValid(false);
        } catch (IOException e) {
            report.addWarning("Could not verify SSL certificate: " + e.getMessage());
        }
    }
    
    /**
     * Check for redirect chains
     */
    private void checkRedirects(URL url, LinkAnalysisReport report) {
        try {
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setInstanceFollowRedirects(false);
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);
            connection.setRequestMethod("HEAD");
            
            int redirectCount = 0;
            String currentUrl = url.toString();
            List<String> redirectChain = new ArrayList<>();
            redirectChain.add(currentUrl);
            
            while (redirectCount < 10) {
                connection = (HttpURLConnection) new URL(currentUrl).openConnection();
                connection.setInstanceFollowRedirects(false);
                connection.setConnectTimeout(5000);
                connection.setReadTimeout(5000);
                connection.setRequestMethod("HEAD");
                
                int responseCode = connection.getResponseCode();
                
                if (responseCode >= 300 && responseCode < 400) {
                    String location = connection.getHeaderField("Location");
                    if (location == null) break;
                    
                    redirectChain.add(location);
                    currentUrl = location;
                    redirectCount++;
                } else {
                    break;
                }
                
                connection.disconnect();
            }
            
            if (redirectCount > 0) {
                report.setRedirectCount(redirectCount);
                report.setRedirectChain(redirectChain);
                
                if (redirectCount > 3) {
                    report.setThreatLevel(ThreatLevel.MEDIUM);
                    report.addWarning("Excessive redirects (" + redirectCount + ")");
                } else {
                    report.addInfo("URL redirects " + redirectCount + " time(s)");
                }
            }
            
        } catch (IOException e) {
            report.addWarning("Could not check redirects: " + e.getMessage());
        }
    }
    
    /**
     * Detect phishing patterns
     */
    private void detectPhishing(String url, LinkAnalysisReport report) {
        for (Pattern pattern : PHISHING_PATTERNS) {
            if (pattern.matcher(url).find()) {
                report.setThreatLevel(ThreatLevel.HIGH);
                report.addThreat("Possible phishing: matches pattern '" + pattern.pattern() + "'");
            }
        }
    }
    
    /**
     * Threat level enum
     */
    public enum ThreatLevel {
        SAFE, LOW, MEDIUM, HIGH, CRITICAL
    }
    
    /**
     * Link analysis report
     */
    public static class LinkAnalysisReport {
        private final String url;
        private String domain;
        private String protocol;
        private ThreatLevel threatLevel = ThreatLevel.SAFE;
        private boolean sslValid = false;
        private String certificateIssuer;
        private int redirectCount = 0;
        private List<String> redirectChain = new ArrayList<>();
        private List<String> threats = new ArrayList<>();
        private List<String> warnings = new ArrayList<>();
        private List<String> info = new ArrayList<>();
        
        public LinkAnalysisReport(String url) {
            this.url = url;
        }
        
        // Getters and setters
        public String getUrl() { return url; }
        public String getDomain() { return domain; }
        public void setDomain(String domain) { this.domain = domain; }
        public String getProtocol() { return protocol; }
        public void setProtocol(String protocol) { this.protocol = protocol; }
        public ThreatLevel getThreatLevel() { return threatLevel; }
        public void setThreatLevel(ThreatLevel level) {
            if (level.ordinal() > this.threatLevel.ordinal()) {
                this.threatLevel = level;
            }
        }
        public boolean isSslValid() { return sslValid; }
        public void setSslValid(boolean sslValid) { this.sslValid = sslValid; }
        public String getCertificateIssuer() { return certificateIssuer; }
        public void setCertificateIssuer(String issuer) { this.certificateIssuer = issuer; }
        public int getRedirectCount() { return redirectCount; }
        public void setRedirectCount(int count) { this.redirectCount = count; }
        public List<String> getRedirectChain() { return redirectChain; }
        public void setRedirectChain(List<String> chain) { this.redirectChain = chain; }
        public List<String> getThreats() { return threats; }
        public void addThreat(String threat) { this.threats.add(threat); }
        public List<String> getWarnings() { return warnings; }
        public void addWarning(String warning) { this.warnings.add(warning); }
        public List<String> getInfo() { return info; }
        public void addInfo(String info) { this.info.add(info); }
        
        /**
         * Generate human-readable report
         */
        public String toFormattedString() {
            StringBuilder sb = new StringBuilder();
            sb.append("═══════════════════════════════════════════════════════\n");
            sb.append("LINK SAFETY REPORT\n");
            sb.append("═══════════════════════════════════════════════════════\n\n");
            
            sb.append("URL: ").append(url).append("\n");
            sb.append("Domain: ").append(domain != null ? domain : "N/A").append("\n");
            sb.append("Protocol: ").append(protocol != null ? protocol.toUpperCase() : "N/A").append("\n");
            
            if (protocol != null && protocol.equalsIgnoreCase("https")) {
                sb.append("SSL Valid: ").append(sslValid ? "✓ Yes" : "✗ No").append("\n");
                if (certificateIssuer != null) {
                    sb.append("Certificate Issuer: ").append(certificateIssuer).append("\n");
                }
            }
            
            if (redirectCount > 0) {
                sb.append("Redirects: ").append(redirectCount).append("\n");
            }
            
            sb.append("\nTHREAT LEVEL: ").append(getThreatLevelColor()).append(threatLevel).append("\u001B[0m\n\n");
            
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
            
            if (!redirectChain.isEmpty() && redirectChain.size() > 1) {
                sb.append("REDIRECT CHAIN:\n");
                for (int i = 0; i < redirectChain.size(); i++) {
                    sb.append("  ").append(i + 1).append(". ").append(redirectChain.get(i)).append("\n");
                }
                sb.append("\n");
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
    }
}
