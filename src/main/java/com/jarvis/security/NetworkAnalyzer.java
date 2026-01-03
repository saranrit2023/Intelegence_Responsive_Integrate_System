package com.jarvis.security;

import java.io.*;
import java.util.*;
import java.util.regex.*;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Network packet capture and analysis using tshark (Wireshark CLI)
 * Provides unauthorized access detection, suspicious activity analysis, and network reports
 */
public class NetworkAnalyzer {
    
    private static final String RESULTS_DIR = "/tmp/iris-network/";
    private static final int DEFAULT_CAPTURE_DURATION = 15; // seconds
    
    // Known safe local network patterns
    private static final Set<String> SAFE_PROTOCOLS = Set.of(
        "TCP", "UDP", "HTTP", "HTTPS", "DNS", "DHCP", "ARP", "ICMP", "TLS", "SSL"
    );
    
    // Suspicious port ranges (commonly used for attacks)
    private static final Set<Integer> SUSPICIOUS_PORTS = Set.of(
        4444, 5555, 6666, 31337, 1234, 12345, 54321, 9999, 8888
    );
    
    public NetworkAnalyzer() {
        try {
            Files.createDirectories(Paths.get(RESULTS_DIR));
        } catch (IOException e) {
            System.err.println("Could not create network results directory: " + e.getMessage());
        }
    }
    
    /**
     * Detect available network interfaces
     */
    public List<String> detectInterfaces() {
        List<String> interfaces = new ArrayList<>();
        try {
            ProcessBuilder pb = new ProcessBuilder("ip", "link", "show");
            pb.redirectErrorStream(true);
            Process process = pb.start();
            
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                String line;
                Pattern pattern = Pattern.compile("^\\d+:\\s+(\\w+):");
                while ((line = reader.readLine()) != null) {
                    Matcher matcher = pattern.matcher(line);
                    if (matcher.find()) {
                        String iface = matcher.group(1);
                        if (!iface.equals("lo")) { // Skip loopback
                            interfaces.add(iface);
                        }
                    }
                }
            }
            process.waitFor();
        } catch (Exception e) {
            System.err.println("Error detecting interfaces: " + e.getMessage());
            // Fallback to common interfaces
            interfaces.addAll(Arrays.asList("eth0", "wlan0", "enp0s3"));
        }
        return interfaces;
    }
    
    /**
     * Get the default/primary network interface
     */
    public String getDefaultInterface() {
        try {
            ProcessBuilder pb = new ProcessBuilder("bash", "-c", 
                "ip route | grep default | awk '{print $5}' | head -1");
            pb.redirectErrorStream(true);
            Process process = pb.start();
            
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                String line = reader.readLine();
                if (line != null && !line.trim().isEmpty()) {
                    return line.trim();
                }
            }
            process.waitFor();
        } catch (Exception e) {
            System.err.println("Error getting default interface: " + e.getMessage());
        }
        
        // Fallback: try common interfaces
        List<String> interfaces = detectInterfaces();
        if (!interfaces.isEmpty()) {
            return interfaces.get(0);
        }
        return "eth0";
    }
    
    /**
     * Capture and analyze network packets
     */
    public NetworkReport captureAndAnalyze(String networkInterface, int duration) {
        if (networkInterface == null || networkInterface.isEmpty()) {
            networkInterface = getDefaultInterface();
        }
        if (duration <= 0) {
            duration = DEFAULT_CAPTURE_DURATION;
        }
        
        System.out.println("\n📡 Starting network capture on " + networkInterface + " for " + duration + " seconds...");
        
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String pcapFile = RESULTS_DIR + "capture_" + timestamp + ".pcap";
        
        // Capture packets
        boolean captureSuccess = capturePackets(networkInterface, duration, pcapFile);
        
        if (!captureSuccess) {
            NetworkReport report = new NetworkReport();
            report.setError("Failed to capture packets. Make sure tshark is installed and you have permissions.");
            return report;
        }
        
        // Analyze the capture
        return analyzeCapture(pcapFile);
    }
    
    /**
     * Capture packets using tshark
     */
    private boolean capturePackets(String networkInterface, int duration, String outputFile) {
        try {
            String command = String.format("sudo timeout %d tshark -i %s -w %s 2>/dev/null",
                duration, networkInterface, outputFile);
            
            ProcessBuilder pb = new ProcessBuilder("bash", "-c", command);
            pb.redirectErrorStream(true);
            Process process = pb.start();
            
            // Read output to prevent blocking
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                while (reader.readLine() != null) {
                    // Consume output
                }
            }
            
            int exitCode = process.waitFor();
            return Files.exists(Paths.get(outputFile));
        } catch (Exception e) {
            System.err.println("Error capturing packets: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Analyze a pcap file
     */
    public NetworkReport analyzeCapture(String pcapFile) {
        NetworkReport report = new NetworkReport();
        report.setCaptureFile(pcapFile);
        
        if (!Files.exists(Paths.get(pcapFile))) {
            report.setError("Capture file not found: " + pcapFile);
            return report;
        }
        
        // Get packet summary
        analyzePacketSummary(pcapFile, report);
        
        // Get conversation statistics
        analyzeConversations(pcapFile, report);
        
        // Detect suspicious activity
        detectSuspiciousActivity(report);
        
        return report;
    }
    
    /**
     * Analyze packet summary from capture
     */
    private void analyzePacketSummary(String pcapFile, NetworkReport report) {
        try {
            // Get protocol hierarchy
            ProcessBuilder pb = new ProcessBuilder("bash", "-c",
                "tshark -r " + pcapFile + " -q -z io,phs 2>/dev/null");
            pb.redirectErrorStream(true);
            Process process = pb.start();
            
            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                    
                    // Parse protocol stats
                    if (line.contains("frames:")) {
                        String[] parts = line.trim().split("\\s+");
                        if (parts.length >= 2) {
                            String protocol = parts[0].replace(":", "");
                            try {
                                int frames = Integer.parseInt(parts[1].replace("frames:", ""));
                                report.addProtocolCount(protocol.toUpperCase(), frames);
                            } catch (NumberFormatException e) {
                                // Ignore parsing errors
                            }
                        }
                    }
                }
            }
            process.waitFor();
            report.setProtocolHierarchy(output.toString());
            
            // Get total packet count
            pb = new ProcessBuilder("bash", "-c",
                "tshark -r " + pcapFile + " 2>/dev/null | wc -l");
            process = pb.start();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                String line = reader.readLine();
                if (line != null) {
                    report.setTotalPackets(Integer.parseInt(line.trim()));
                }
            }
            process.waitFor();
            
        } catch (Exception e) {
            System.err.println("Error analyzing packets: " + e.getMessage());
        }
    }
    
    /**
     * Analyze network conversations
     */
    private void analyzeConversations(String pcapFile, NetworkReport report) {
        try {
            // Get IP conversations
            ProcessBuilder pb = new ProcessBuilder("bash", "-c",
                "tshark -r " + pcapFile + " -q -z conv,ip 2>/dev/null");
            pb.redirectErrorStream(true);
            Process process = pb.start();
            
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                String line;
                boolean inTable = false;
                while ((line = reader.readLine()) != null) {
                    if (line.contains("<->")) {
                        inTable = true;
                        // Parse: IP_A <-> IP_B  frames bytes
                        String[] parts = line.trim().split("\\s+");
                        if (parts.length >= 2) {
                            String ipA = parts[0];
                            String ipB = parts[2]; // After <->
                            report.addActiveIP(ipA);
                            report.addActiveIP(ipB);
                            
                            // Count frames for traffic analysis
                            if (parts.length >= 4) {
                                try {
                                    int frames = Integer.parseInt(parts[3]);
                                    report.addIPTraffic(ipA, frames);
                                    report.addIPTraffic(ipB, frames);
                                } catch (NumberFormatException e) {
                                    // Ignore
                                }
                            }
                        }
                    }
                }
            }
            process.waitFor();
            
        } catch (Exception e) {
            System.err.println("Error analyzing conversations: " + e.getMessage());
        }
    }
    
    /**
     * Detect suspicious activity in the captured traffic
     */
    private void detectSuspiciousActivity(NetworkReport report) {
        List<String> findings = new ArrayList<>();
        
        // Check for unusual protocols
        for (Map.Entry<String, Integer> entry : report.getProtocolCounts().entrySet()) {
            String protocol = entry.getKey();
            if (!SAFE_PROTOCOLS.contains(protocol) && entry.getValue() > 10) {
                findings.add("⚠️ Unusual protocol detected: " + protocol + " (" + entry.getValue() + " packets)");
            }
        }
        
        // Check for high traffic from unknown IPs
        for (Map.Entry<String, Integer> entry : report.getIpTraffic().entrySet()) {
            String ip = entry.getKey();
            int traffic = entry.getValue();
            
            // Flag IPs with very high traffic
            if (traffic > 1000) {
                findings.add("⚠️ High traffic from IP: " + ip + " (" + traffic + " packets)");
            }
            
            // Flag external IPs (non-private ranges)
            if (!isPrivateIP(ip) && traffic > 100) {
                findings.add("🌐 External IP with significant traffic: " + ip);
            }
        }
        
        report.setSuspiciousFindings(findings);
    }
    
    /**
     * Check for unauthorized access / suspicious network activity
     */
    public NetworkReport detectUnauthorizedAccess(String networkInterface, int duration) {
        if (networkInterface == null || networkInterface.isEmpty()) {
            networkInterface = getDefaultInterface();
        }
        if (duration <= 0) {
            duration = 20; // Longer capture for security analysis
        }
        
        System.out.println("\n🔒 Scanning for unauthorized access on " + networkInterface + "...");
        
        NetworkReport report = captureAndAnalyze(networkInterface, duration);
        
        // Additional security checks
        List<String> securityFindings = new ArrayList<>(report.getSuspiciousFindings());
        
        // Check for port scanning
        detectPortScans(report, securityFindings);
        
        // Check for ARP spoofing
        detectARPSpoofing(networkInterface, securityFindings);
        
        // Check for DNS anomalies
        checkDNSAnomalies(report, securityFindings);
        
        report.setSuspiciousFindings(securityFindings);
        report.setSecurityScan(true);
        
        return report;
    }
    
    /**
     * Detect potential port scanning activity
     */
    private void detectPortScans(NetworkReport report, List<String> findings) {
        // Check if any IP is contacting many different ports
        Map<String, Set<Integer>> ipToPorts = new HashMap<>();
        
        for (String ip : report.getActiveIPs()) {
            // This is a simplified check - in production you'd parse actual port data
            int traffic = report.getIpTraffic().getOrDefault(ip, 0);
            if (traffic > 50 && !isPrivateIP(ip)) {
                findings.add("🔍 Potential port scan from: " + ip);
            }
        }
    }
    
    /**
     * Detect ARP spoofing attempts
     */
    private void detectARPSpoofing(String networkInterface, List<String> findings) {
        try {
            ProcessBuilder pb = new ProcessBuilder("bash", "-c",
                "arp -a 2>/dev/null | awk '{print $4}' | sort | uniq -d");
            pb.redirectErrorStream(true);
            Process process = pb.start();
            
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (!line.trim().isEmpty()) {
                        findings.add("🚨 ALERT: Possible ARP spoofing detected! Duplicate MAC: " + line.trim());
                    }
                }
            }
            process.waitFor();
        } catch (Exception e) {
            System.err.println("Error checking ARP table: " + e.getMessage());
        }
    }
    
    /**
     * Check for DNS anomalies
     */
    private void checkDNSAnomalies(NetworkReport report, List<String> findings) {
        Integer dnsCount = report.getProtocolCounts().get("DNS");
        if (dnsCount != null && dnsCount > 500) {
            findings.add("⚠️ High DNS traffic detected (" + dnsCount + " queries) - possible DNS tunneling");
        }
    }
    
    /**
     * Generate comprehensive network report
     */
    public String getNetworkReport(String networkInterface) {
        if (networkInterface == null || networkInterface.isEmpty()) {
            networkInterface = getDefaultInterface();
        }
        
        StringBuilder report = new StringBuilder();
        report.append("\n📊 NETWORK STATUS REPORT\n");
        report.append("═══════════════════════════════════════\n\n");
        
        // Interface info
        report.append("🌐 Interface: ").append(networkInterface).append("\n");
        
        // Get IP address
        try {
            ProcessBuilder pb = new ProcessBuilder("bash", "-c",
                "ip addr show " + networkInterface + " | grep 'inet ' | awk '{print $2}'");
            Process process = pb.start();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                String ip = reader.readLine();
                if (ip != null) {
                    report.append("📍 IP Address: ").append(ip.trim()).append("\n");
                }
            }
            process.waitFor();
        } catch (Exception e) {
            // Ignore
        }
        
        // Get gateway
        try {
            ProcessBuilder pb = new ProcessBuilder("bash", "-c",
                "ip route | grep default | awk '{print $3}'");
            Process process = pb.start();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                String gateway = reader.readLine();
                if (gateway != null) {
                    report.append("🚪 Gateway: ").append(gateway.trim()).append("\n");
                }
            }
            process.waitFor();
        } catch (Exception e) {
            // Ignore
        }
        
        // Get connected devices (ARP table)
        report.append("\n📱 Connected Devices:\n");
        try {
            ProcessBuilder pb = new ProcessBuilder("bash", "-c",
                "arp -a 2>/dev/null | head -10");
            Process process = pb.start();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                String line;
                int count = 0;
                while ((line = reader.readLine()) != null && count < 10) {
                    report.append("   • ").append(line.trim()).append("\n");
                    count++;
                }
            }
            process.waitFor();
        } catch (Exception e) {
            report.append("   Could not retrieve device list\n");
        }
        
        // Network statistics
        report.append("\n📈 Network Statistics:\n");
        try {
            ProcessBuilder pb = new ProcessBuilder("bash", "-c",
                "cat /proc/net/dev | grep " + networkInterface);
            Process process = pb.start();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                String line = reader.readLine();
                if (line != null) {
                    String[] parts = line.trim().split("\\s+");
                    if (parts.length >= 10) {
                        long rxBytes = Long.parseLong(parts[1]);
                        long txBytes = Long.parseLong(parts[9]);
                        report.append("   📥 Received: ").append(formatBytes(rxBytes)).append("\n");
                        report.append("   📤 Transmitted: ").append(formatBytes(txBytes)).append("\n");
                    }
                }
            }
            process.waitFor();
        } catch (Exception e) {
            // Ignore
        }
        
        report.append("\n═══════════════════════════════════════\n");
        
        return report.toString();
    }
    
    /**
     * Check if IP is in private range
     */
    private boolean isPrivateIP(String ip) {
        if (ip == null) return false;
        return ip.startsWith("192.168.") ||
               ip.startsWith("10.") ||
               ip.startsWith("172.16.") ||
               ip.startsWith("172.17.") ||
               ip.startsWith("172.18.") ||
               ip.startsWith("172.19.") ||
               ip.startsWith("172.2") ||
               ip.startsWith("172.30.") ||
               ip.startsWith("172.31.") ||
               ip.startsWith("127.") ||
               ip.equals("localhost");
    }
    
    /**
     * Format bytes to human readable
     */
    private String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024));
        return String.format("%.1f GB", bytes / (1024.0 * 1024 * 1024));
    }
    
    // ==================== ADVANCED PENTESTING FEATURES ====================
    
    /**
     * Capture and analyze HTTP traffic - useful for bug bounty
     */
    public String analyzeHTTPTraffic(String networkInterface, int duration) {
        if (networkInterface == null) networkInterface = getDefaultInterface();
        if (duration <= 0) duration = 20;
        
        System.out.println("\n🌐 Capturing HTTP traffic for analysis...");
        StringBuilder result = new StringBuilder();
        result.append("\n🌐 HTTP TRAFFIC ANALYSIS\n");
        result.append("═══════════════════════════════════════\n\n");
        
        try {
            // Capture HTTP requests
            String cmd = String.format(
                "sudo timeout %d tshark -i %s -Y 'http.request or http.response' -T fields " +
                "-e ip.src -e ip.dst -e http.host -e http.request.uri -e http.request.method " +
                "-e http.response.code -e http.cookie 2>/dev/null | head -50",
                duration, networkInterface);
            
            ProcessBuilder pb = new ProcessBuilder("bash", "-c", cmd);
            Process process = pb.start();
            
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                String line;
                int count = 0;
                while ((line = reader.readLine()) != null && count < 50) {
                    if (!line.trim().isEmpty()) {
                        String[] parts = line.split("\t");
                        if (parts.length >= 4) {
                            result.append("📍 ").append(parts[0]).append(" → ").append(parts[1]).append("\n");
                            if (parts.length > 2 && !parts[2].isEmpty()) 
                                result.append("   Host: ").append(parts[2]).append("\n");
                            if (parts.length > 3 && !parts[3].isEmpty()) 
                                result.append("   URI: ").append(parts[3]).append("\n");
                            if (parts.length > 4 && !parts[4].isEmpty()) 
                                result.append("   Method: ").append(parts[4]).append("\n");
                            result.append("\n");
                        }
                        count++;
                    }
                }
            }
            process.waitFor();
            
        } catch (Exception e) {
            result.append("Error: ").append(e.getMessage()).append("\n");
        }
        
        result.append("═══════════════════════════════════════\n");
        return result.toString();
    }
    
    /**
     * Extract credentials from network traffic (cleartext passwords, cookies)
     */
    public String extractCredentials(String networkInterface, int duration) {
        if (networkInterface == null) networkInterface = getDefaultInterface();
        if (duration <= 0) duration = 30;
        
        System.out.println("\n🔑 Scanning for credentials in network traffic...");
        StringBuilder result = new StringBuilder();
        result.append("\n🔑 CREDENTIAL EXTRACTION REPORT\n");
        result.append("═══════════════════════════════════════\n\n");
        
        List<String> credentials = new ArrayList<>();
        
        try {
            // Look for HTTP Basic Auth
            capturePattern(networkInterface, duration, "http.authorization", credentials, "HTTP Auth");
            
            // Look for FTP credentials
            capturePattern(networkInterface, duration, "ftp.request.command == USER or ftp.request.command == PASS", 
                credentials, "FTP");
            
            // Look for cookies
            capturePattern(networkInterface, duration, "http.cookie", credentials, "Cookie");
            
            // Look for POST data (form submissions)
            capturePattern(networkInterface, duration, 
                "http.request.method == POST and (urlencoded-form or http.file_data)", 
                credentials, "POST Data");
            
        } catch (Exception e) {
            result.append("Error: ").append(e.getMessage()).append("\n");
        }
        
        if (credentials.isEmpty()) {
            result.append("✅ No cleartext credentials detected in captured traffic.\n");
        } else {
            result.append("⚠️ FOUND ").append(credentials.size()).append(" potential credential(s):\n\n");
            for (String cred : credentials) {
                result.append("   • ").append(cred).append("\n");
            }
        }
        
        result.append("\n═══════════════════════════════════════\n");
        return result.toString();
    }
    
    private void capturePattern(String iface, int duration, String filter, 
                                List<String> results, String type) {
        try {
            String cmd = String.format(
                "sudo timeout %d tshark -i %s -Y '%s' -T fields -e ip.src -e ip.dst " +
                "-e frame.time 2>/dev/null | head -20", duration, iface, filter);
            
            ProcessBuilder pb = new ProcessBuilder("bash", "-c", cmd);
            Process process = pb.start();
            
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (!line.trim().isEmpty()) {
                        results.add("[" + type + "] " + line.trim());
                    }
                }
            }
            process.waitFor();
        } catch (Exception e) {
            System.err.println("Error capturing pattern: " + e.getMessage());
        }
    }
    
    /**
     * Analyze DNS queries - detect exfiltration, suspicious domains
     */
    public String analyzeDNS(String networkInterface, int duration) {
        if (networkInterface == null) networkInterface = getDefaultInterface();
        if (duration <= 0) duration = 20;
        
        System.out.println("\n🔍 Analyzing DNS traffic...");
        StringBuilder result = new StringBuilder();
        result.append("\n🔍 DNS ANALYSIS REPORT\n");
        result.append("═══════════════════════════════════════\n\n");
        
        try {
            String cmd = String.format(
                "sudo timeout %d tshark -i %s -Y 'dns.flags.response == 0' -T fields " +
                "-e ip.src -e dns.qry.name -e dns.qry.type 2>/dev/null | sort | uniq -c | sort -rn | head -30",
                duration, networkInterface);
            
            ProcessBuilder pb = new ProcessBuilder("bash", "-c", cmd);
            Process process = pb.start();
            
            result.append("📋 Top DNS Queries:\n\n");
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                String line;
                List<String> suspiciousDomains = new ArrayList<>();
                
                while ((line = reader.readLine()) != null) {
                    if (!line.trim().isEmpty()) {
                        result.append("   ").append(line.trim()).append("\n");
                        
                        // Check for suspicious patterns
                        if (line.contains(".xyz") || line.contains(".tk") || 
                            line.contains(".ml") || line.length() > 60) {
                            suspiciousDomains.add(line.trim());
                        }
                    }
                }
                
                if (!suspiciousDomains.isEmpty()) {
                    result.append("\n⚠️ Suspicious Domains Detected:\n");
                    for (String domain : suspiciousDomains) {
                        result.append("   🚨 ").append(domain).append("\n");
                    }
                }
            }
            process.waitFor();
            
        } catch (Exception e) {
            result.append("Error: ").append(e.getMessage()).append("\n");
        }
        
        result.append("\n═══════════════════════════════════════\n");
        return result.toString();
    }
    
    /**
     * Follow and display TCP stream - like Wireshark's "Follow TCP Stream"
     */
    public String followTCPStream(String networkInterface, String targetIP, int duration) {
        if (networkInterface == null) networkInterface = getDefaultInterface();
        if (duration <= 0) duration = 15;
        
        System.out.println("\n📡 Following TCP streams...");
        StringBuilder result = new StringBuilder();
        result.append("\n📡 TCP STREAM ANALYSIS\n");
        result.append("═══════════════════════════════════════\n\n");
        
        try {
            String filter = targetIP != null ? 
                String.format("ip.addr == %s", targetIP) : "tcp";
            
            String cmd = String.format(
                "sudo timeout %d tshark -i %s -Y '%s' -z follow,tcp,ascii,0 2>/dev/null | head -100",
                duration, networkInterface, filter);
            
            ProcessBuilder pb = new ProcessBuilder("bash", "-c", cmd);
            Process process = pb.start();
            
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    result.append(line).append("\n");
                }
            }
            process.waitFor();
            
        } catch (Exception e) {
            result.append("Error: ").append(e.getMessage()).append("\n");
        }
        
        result.append("\n═══════════════════════════════════════\n");
        return result.toString();
    }
    
    /**
     * Detect sensitive data leaks - API keys, tokens, passwords in traffic
     */
    public String detectSensitiveDataLeaks(String networkInterface, int duration) {
        if (networkInterface == null) networkInterface = getDefaultInterface();
        if (duration <= 0) duration = 30;
        
        System.out.println("\n🔐 Scanning for sensitive data leaks...");
        StringBuilder result = new StringBuilder();
        result.append("\n🔐 SENSITIVE DATA LEAK DETECTION\n");
        result.append("═══════════════════════════════════════\n\n");
        
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String pcapFile = RESULTS_DIR + "sensitive_scan_" + timestamp + ".pcap";
        
        // Capture traffic
        capturePackets(networkInterface, duration, pcapFile);
        
        List<String> leaks = new ArrayList<>();
        
        // Search for common sensitive patterns
        String[] patterns = {
            "api[_-]?key", "apikey", "api_secret",
            "password", "passwd", "pwd",
            "token", "bearer", "auth",
            "secret", "private[_-]?key",
            "aws[_-]?access", "aws[_-]?secret"
        };
        
        try {
            for (String pattern : patterns) {
                String cmd = String.format(
                    "tshark -r %s -Y 'http' -T fields -e http.file_data -e http.request.uri " +
                    "-e http.cookie 2>/dev/null | grep -iE '%s' | head -10", pcapFile, pattern);
                
                ProcessBuilder pb = new ProcessBuilder("bash", "-c", cmd);
                Process process = pb.start();
                
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(process.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        if (!line.trim().isEmpty()) {
                            leaks.add("[" + pattern + "] " + truncate(line.trim(), 100));
                        }
                    }
                }
                process.waitFor();
            }
        } catch (Exception e) {
            result.append("Error: ").append(e.getMessage()).append("\n");
        }
        
        if (leaks.isEmpty()) {
            result.append("✅ No obvious sensitive data leaks detected.\n");
        } else {
            result.append("🚨 FOUND ").append(leaks.size()).append(" potential leak(s):\n\n");
            for (String leak : leaks) {
                result.append("   ⚠️ ").append(leak).append("\n");
            }
        }
        
        result.append("\n📁 Full capture saved: ").append(pcapFile);
        result.append("\n═══════════════════════════════════════\n");
        return result.toString();
    }
    
    private String truncate(String str, int maxLen) {
        if (str.length() <= maxLen) return str;
        return str.substring(0, maxLen) + "...";
    }
    
    /**
     * SSL/TLS certificate analysis
     */
    public String analyzeSSLCertificates(String networkInterface, int duration) {
        if (networkInterface == null) networkInterface = getDefaultInterface();
        if (duration <= 0) duration = 20;
        
        System.out.println("\n🔒 Analyzing SSL/TLS certificates...");
        StringBuilder result = new StringBuilder();
        result.append("\n🔒 SSL/TLS CERTIFICATE ANALYSIS\n");
        result.append("═══════════════════════════════════════\n\n");
        
        try {
            String cmd = String.format(
                "sudo timeout %d tshark -i %s -Y 'ssl.handshake.certificate' -T fields " +
                "-e ip.dst -e x509sat.uTF8String -e x509ce.dNSName 2>/dev/null | head -30",
                duration, networkInterface);
            
            ProcessBuilder pb = new ProcessBuilder("bash", "-c", cmd);
            Process process = pb.start();
            
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (!line.trim().isEmpty()) {
                        String[] parts = line.split("\t");
                        result.append("📍 Server: ").append(parts[0]).append("\n");
                        if (parts.length > 1) result.append("   Issuer: ").append(parts[1]).append("\n");
                        if (parts.length > 2) result.append("   Domain: ").append(parts[2]).append("\n");
                        result.append("\n");
                    }
                }
            }
            process.waitFor();
            
        } catch (Exception e) {
            result.append("Error: ").append(e.getMessage()).append("\n");
        }
        
        result.append("═══════════════════════════════════════\n");
        return result.toString();
    }
    
    /**
     * Extract files/objects from HTTP traffic
     */
    public String extractHTTPObjects(String networkInterface, int duration) {
        if (networkInterface == null) networkInterface = getDefaultInterface();
        if (duration <= 0) duration = 30;
        
        System.out.println("\n📦 Extracting objects from HTTP traffic...");
        String exportDir = RESULTS_DIR + "extracted_" + System.currentTimeMillis() + "/";
        
        StringBuilder result = new StringBuilder();
        result.append("\n📦 HTTP OBJECT EXTRACTION\n");
        result.append("═══════════════════════════════════════\n\n");
        
        try {
            Files.createDirectories(Paths.get(exportDir));
            
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String pcapFile = RESULTS_DIR + "http_extract_" + timestamp + ".pcap";
            
            // Capture traffic
            capturePackets(networkInterface, duration, pcapFile);
            
            // Export HTTP objects
            String cmd = String.format(
                "tshark -r %s --export-objects http,%s 2>/dev/null", pcapFile, exportDir);
            
            ProcessBuilder pb = new ProcessBuilder("bash", "-c", cmd);
            Process process = pb.start();
            process.waitFor();
            
            // List extracted files
            File dir = new File(exportDir);
            String[] files = dir.list();
            
            if (files != null && files.length > 0) {
                result.append("✅ Extracted ").append(files.length).append(" file(s):\n\n");
                for (String file : files) {
                    File f = new File(exportDir + file);
                    result.append("   📄 ").append(file)
                          .append(" (").append(formatBytes(f.length())).append(")\n");
                }
                result.append("\n📁 Saved to: ").append(exportDir);
            } else {
                result.append("No HTTP objects found in captured traffic.\n");
            }
            
        } catch (Exception e) {
            result.append("Error: ").append(e.getMessage()).append("\n");
        }
        
        result.append("\n═══════════════════════════════════════\n");
        return result.toString();
    }
    
    /**
     * Full bug bounty scan - combines multiple checks
     */
    public String bugBountyScan(String networkInterface, int duration) {
        if (networkInterface == null) networkInterface = getDefaultInterface();
        if (duration <= 0) duration = 45;
        
        System.out.println("\n🎯 Starting comprehensive bug bounty network scan...");
        StringBuilder result = new StringBuilder();
        result.append("\n🎯 BUG BOUNTY NETWORK SCAN\n");
        result.append("═══════════════════════════════════════\n\n");
        
        // Capture first
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String pcapFile = RESULTS_DIR + "bugbounty_" + timestamp + ".pcap";
        
        System.out.println("📡 Capturing traffic for " + duration + " seconds...");
        capturePackets(networkInterface, duration, pcapFile);
        
        // Analyze
        NetworkReport report = analyzeCapture(pcapFile);
        result.append("📊 Captured ").append(report.getTotalPackets()).append(" packets\n\n");
        
        // Check for sensitive data
        result.append("🔍 Checking for sensitive data exposure...\n");
        int issues = 0;
        
        // Quick checks on pcap
        try {
            // Check for cleartext passwords
            issues += checkPattern(pcapFile, "password=", result, "Cleartext password");
            issues += checkPattern(pcapFile, "api_key=", result, "API key exposure");
            issues += checkPattern(pcapFile, "token=", result, "Token exposure");
            issues += checkPattern(pcapFile, "Authorization: Basic", result, "Basic Auth");
            
        } catch (Exception e) {
            result.append("Error during analysis: ").append(e.getMessage()).append("\n");
        }
        
        result.append("\n📋 SUMMARY:\n");
        result.append("   • Packets analyzed: ").append(report.getTotalPackets()).append("\n");
        result.append("   • Active IPs: ").append(report.getActiveIPs().size()).append("\n");
        result.append("   • Issues found: ").append(issues).append("\n");
        result.append("   • Capture file: ").append(pcapFile).append("\n");
        
        result.append("\n═══════════════════════════════════════\n");
        return result.toString();
    }
    
    private int checkPattern(String pcapFile, String pattern, StringBuilder result, String desc) {
        try {
            String cmd = String.format(
                "tshark -r %s -Y 'http' -T fields -e http.file_data -e http.request.uri " +
                "2>/dev/null | grep -c '%s' 2>/dev/null || echo 0", pcapFile, pattern);
            
            ProcessBuilder pb = new ProcessBuilder("bash", "-c", cmd);
            Process process = pb.start();
            
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                String line = reader.readLine();
                if (line != null) {
                    int count = Integer.parseInt(line.trim());
                    if (count > 0) {
                        result.append("   ⚠️ ").append(desc).append(": ").append(count).append(" occurrence(s)\n");
                        return count;
                    }
                }
            }
            process.waitFor();
        } catch (Exception e) {
            // Ignore
        }
        return 0;
    }
    
    /**
     * Network analysis report class
     */
    public static class NetworkReport {
        private String captureFile;
        private int totalPackets;
        private String protocolHierarchy;
        private Map<String, Integer> protocolCounts = new HashMap<>();
        private Set<String> activeIPs = new HashSet<>();
        private Map<String, Integer> ipTraffic = new HashMap<>();
        private List<String> suspiciousFindings = new ArrayList<>();
        private boolean securityScan = false;
        private String error;
        
        public void setCaptureFile(String file) { this.captureFile = file; }
        public String getCaptureFile() { return captureFile; }
        
        public void setTotalPackets(int count) { this.totalPackets = count; }
        public int getTotalPackets() { return totalPackets; }
        
        public void setProtocolHierarchy(String hierarchy) { this.protocolHierarchy = hierarchy; }
        
        public void addProtocolCount(String protocol, int count) {
            protocolCounts.merge(protocol, count, Integer::sum);
        }
        public Map<String, Integer> getProtocolCounts() { return protocolCounts; }
        
        public void addActiveIP(String ip) { activeIPs.add(ip); }
        public Set<String> getActiveIPs() { return activeIPs; }
        
        public void addIPTraffic(String ip, int packets) {
            ipTraffic.merge(ip, packets, Integer::sum);
        }
        public Map<String, Integer> getIpTraffic() { return ipTraffic; }
        
        public void setSuspiciousFindings(List<String> findings) { this.suspiciousFindings = findings; }
        public List<String> getSuspiciousFindings() { return suspiciousFindings; }
        
        public void setSecurityScan(boolean security) { this.securityScan = security; }
        public boolean isSecurityScan() { return securityScan; }
        
        public void setError(String error) { this.error = error; }
        public String getError() { return error; }
        
        public boolean hasError() { return error != null && !error.isEmpty(); }
        
        /**
         * Generate formatted report string
         */
        public String toFormattedString() {
            StringBuilder sb = new StringBuilder();
            
            if (hasError()) {
                sb.append("❌ Error: ").append(error).append("\n");
                return sb.toString();
            }
            
            if (securityScan) {
                sb.append("\n🔒 SECURITY SCAN REPORT\n");
            } else {
                sb.append("\n📡 NETWORK ANALYSIS REPORT\n");
            }
            sb.append("═══════════════════════════════════════\n\n");
            
            sb.append("📊 Total Packets Captured: ").append(totalPackets).append("\n");
            sb.append("📁 Capture File: ").append(captureFile).append("\n\n");
            
            // Active IPs
            sb.append("🌐 Active IPs: ").append(activeIPs.size()).append("\n");
            int count = 0;
            for (String ip : activeIPs) {
                if (count++ < 10) {
                    int traffic = ipTraffic.getOrDefault(ip, 0);
                    sb.append("   • ").append(ip).append(" (").append(traffic).append(" packets)\n");
                }
            }
            if (activeIPs.size() > 10) {
                sb.append("   ... and ").append(activeIPs.size() - 10).append(" more\n");
            }
            
            // Protocol breakdown
            if (!protocolCounts.isEmpty()) {
                sb.append("\n📋 Protocol Breakdown:\n");
                protocolCounts.entrySet().stream()
                    .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
                    .limit(8)
                    .forEach(e -> sb.append("   • ").append(e.getKey())
                        .append(": ").append(e.getValue()).append(" packets\n"));
            }
            
            // Security findings
            sb.append("\n");
            if (suspiciousFindings.isEmpty()) {
                sb.append("✅ No suspicious activity detected\n");
            } else {
                sb.append("⚠️ FINDINGS (").append(suspiciousFindings.size()).append("):\n");
                for (String finding : suspiciousFindings) {
                    sb.append("   ").append(finding).append("\n");
                }
            }
            
            sb.append("\n═══════════════════════════════════════\n");
            
            return sb.toString();
        }
    }
}
