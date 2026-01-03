package com.jarvis.gui;

import com.jarvis.commands.CommandHandler;
import com.jarvis.ai.AIProcessor;
import com.jarvis.speech.TextToSpeech;
import com.jarvis.speech.SpeechRecognizer;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Premium Modern GUI for I.R.I.S Voice Assistant
 * Cyberpunk/Futuristic Theme with Glassmorphism
 */
public class JarvisGUI extends JFrame {
    private final CommandHandler commandHandler;
    private final TextToSpeech tts;
    private final SpeechRecognizer speechRecognizer;
    private final AIProcessor aiProcessor;
    
    private JTextPane chatArea;
    private JTextField inputField;
    private JButton sendButton;
    private JButton voiceButton;
    private JButton grokButton;      // Grok AI button (primary)
    private JButton geminiButton;    // Gemini AI button (secondary)
    private JButton ollamaButton;    // Ollama AI button (tertiary)
    private JLabel statusLabel;
    private JLabel networkStatusLabel;
    private JPanel statusIndicatorPanel;
    private JProgressBar audioLevelBar;
    private volatile boolean isListening = false;
    
    // Premium Cyberpunk Color Palette
    private static final Color BG_DARK = new Color(8, 8, 12);           // Deep space black
    private static final Color BG_CARD = new Color(15, 15, 25);          // Card background
    private static final Color BG_INPUT = new Color(20, 20, 35);         // Input field bg
    
    // Neon accent colors
    private static final Color NEON_CYAN = new Color(0, 255, 255);       // Cyan glow
    private static final Color NEON_PURPLE = new Color(180, 100, 255);   // Purple accent
    private static final Color NEON_PINK = new Color(255, 0, 128);       // Pink highlight
    private static final Color NEON_BLUE = new Color(50, 150, 255);      // Electric blue
    
    // Gradient colors
    private static final Color GRADIENT_START = new Color(20, 120, 200); // Blue
    private static final Color GRADIENT_END = new Color(140, 80, 220);   // Purple
    
    // Text colors
    private static final Color TEXT_PRIMARY = new Color(240, 240, 255);   // Bright white-blue
    private static final Color TEXT_SECONDARY = new Color(150, 160, 180); // Muted gray-blue
    private static final Color TEXT_ACCENT = new Color(100, 220, 255);    // Cyan text
    
    // Status colors
    private static final Color STATUS_ONLINE = new Color(50, 255, 150);   // Green
    private static final Color STATUS_OFFLINE = new Color(255, 100, 100); // Red
    private static final Color STATUS_SLOW = new Color(255, 200, 50);     // Yellow
    
    public JarvisGUI() {
        this.tts = new TextToSpeech();
        this.speechRecognizer = new SpeechRecognizer();
        this.commandHandler = new CommandHandler(tts);
        this.aiProcessor = commandHandler.getAIProcessor();
        
        initializeUI();
        addWelcomeMessage();
        setupKeyboardShortcuts();
        startNetworkStatusUpdater();
        
        // Welcome speech
        tts.speak("Hello! I am I.R.I.S, your Intelligent Responsive Integrated System. How may I help you?");
    }
    
    private void initializeUI() {
        setTitle("I.R.I.S - Intelligent Responsive Integrated System");
        setSize(1000, 800);
        setMinimumSize(new Dimension(800, 600));
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        
        // Modern look and feel
        try {
            UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        // Main container with gradient background
        JPanel mainContainer = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                
                // Create gradient background
                GradientPaint gradient = new GradientPaint(
                    0, 0, BG_DARK,
                    getWidth(), getHeight(), new Color(15, 10, 30)
                );
                g2d.setPaint(gradient);
                g2d.fillRect(0, 0, getWidth(), getHeight());
                
                // Add subtle grid pattern
                g2d.setColor(new Color(50, 50, 80, 20));
                for (int i = 0; i < getWidth(); i += 50) {
                    g2d.drawLine(i, 0, i, getHeight());
                }
                for (int i = 0; i < getHeight(); i += 50) {
                    g2d.drawLine(0, i, getWidth(), i);
                }
            }
        };
        mainContainer.setBorder(new EmptyBorder(20, 25, 20, 25));
        
        // Header Panel
        JPanel headerPanel = createHeader();
        mainContainer.add(headerPanel, BorderLayout.NORTH);
        
        // Chat Panel (Center)
        JPanel chatPanel = createChatPanel();
        mainContainer.add(chatPanel, BorderLayout.CENTER);
        
        // Bottom Panel (Input + Status)
        JPanel bottomPanel = new JPanel(new BorderLayout(0, 10));
        bottomPanel.setOpaque(false);
        
        JPanel inputPanel = createInputPanel();
        bottomPanel.add(inputPanel, BorderLayout.CENTER);
        
        JPanel statusPanel = createStatusBar();
        bottomPanel.add(statusPanel, BorderLayout.SOUTH);
        
        mainContainer.add(bottomPanel, BorderLayout.SOUTH);
        
        setContentPane(mainContainer);
        
        // Focus on input field after window is visible
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowOpened(WindowEvent e) {
                SwingUtilities.invokeLater(() -> {
                    inputField.requestFocusInWindow();
                });
            }
        });
    }
    
    private JPanel createHeader() {
        JPanel header = new JPanel(new BorderLayout(20, 0));
        header.setOpaque(false);
        header.setBorder(new EmptyBorder(0, 0, 20, 0));
        
        // Left side - Logo and Title
        JPanel titlePanel = new JPanel();
        titlePanel.setLayout(new BoxLayout(titlePanel, BoxLayout.Y_AXIS));
        titlePanel.setOpaque(false);
        
        // Main title with glow effect
        JLabel titleLabel = new JLabel("I.R.I.S") {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, 
                    RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                
                // Glow effect
                g2d.setColor(new Color(0, 200, 255, 50));
                g2d.setFont(getFont());
                for (int i = 0; i < 3; i++) {
                    g2d.drawString(getText(), i, getHeight() - 10 + i);
                }
                
                // Main text with gradient
                GradientPaint textGradient = new GradientPaint(
                    0, 0, NEON_CYAN,
                    getWidth(), 0, NEON_PURPLE
                );
                g2d.setPaint(textGradient);
                g2d.drawString(getText(), 0, getHeight() - 10);
            }
        };
        titleLabel.setFont(new Font("Arial", Font.BOLD, 52));
        titleLabel.setForeground(NEON_CYAN);
        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        JLabel subtitleLabel = new JLabel("Intelligent Responsive Integrated System");
        subtitleLabel.setFont(new Font("Arial", Font.PLAIN, 14));
        subtitleLabel.setForeground(TEXT_SECONDARY);
        subtitleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        titlePanel.add(titleLabel);
        titlePanel.add(Box.createVerticalStrut(5));
        titlePanel.add(subtitleLabel);
        
        // Right side - Controls
        JPanel controlsPanel = new JPanel();
        controlsPanel.setLayout(new BoxLayout(controlsPanel, BoxLayout.Y_AXIS));
        controlsPanel.setOpaque(false);
        
        // AI Mode Buttons Panel - Three buttons: Grok → Gemini → Ollama
        JPanel aiButtonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        aiButtonPanel.setOpaque(false);
        
        // Grok Button (primary/default)
        grokButton = createGlowButton("🎯 Grok", new Color(255, 100, 50));
        grokButton.setPreferredSize(new Dimension(90, 32));
        grokButton.addActionListener(e -> switchToGrok());
        
        // Gemini Button (secondary)
        geminiButton = createGlowButton("🌐 Gemini", NEON_BLUE);
        geminiButton.setPreferredSize(new Dimension(90, 32));
        geminiButton.addActionListener(e -> switchToGemini());
        
        // Ollama Button (tertiary/offline)
        ollamaButton = createGlowButton("🤖 Ollama", new Color(50, 200, 100));
        ollamaButton.setPreferredSize(new Dimension(90, 32));
        ollamaButton.addActionListener(e -> switchToOllama());
        
        // Update button appearances based on current mode
        updateAIButtonStates();
        
        aiButtonPanel.add(grokButton);
        aiButtonPanel.add(geminiButton);
        aiButtonPanel.add(ollamaButton);
        aiButtonPanel.setAlignmentX(Component.RIGHT_ALIGNMENT);
        
        // Network Status
        JPanel networkPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        networkPanel.setOpaque(false);
        
        statusIndicatorPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, 
                    RenderingHints.VALUE_ANTIALIAS_ON);
                
                // Pulsing glow effect
                Color statusColor = getStatusColor();
                g2d.setColor(new Color(statusColor.getRed(), statusColor.getGreen(), 
                    statusColor.getBlue(), 100));
                g2d.fillOval(-2, -2, 16, 16);
                g2d.setColor(statusColor);
                g2d.fillOval(2, 2, 8, 8);
            }
        };
        statusIndicatorPanel.setPreferredSize(new Dimension(12, 12));
        statusIndicatorPanel.setOpaque(false);
        
        networkStatusLabel = new JLabel(aiProcessor.getNetworkStatus());
        networkStatusLabel.setFont(new Font("Arial", Font.PLAIN, 12));
        networkStatusLabel.setForeground(TEXT_SECONDARY);
        
        networkPanel.add(statusIndicatorPanel);
        networkPanel.add(networkStatusLabel);
        networkPanel.setAlignmentX(Component.RIGHT_ALIGNMENT);
        
        controlsPanel.add(aiButtonPanel);
        controlsPanel.add(Box.createVerticalStrut(8));
        controlsPanel.add(networkPanel);
        
        header.add(titlePanel, BorderLayout.WEST);
        header.add(controlsPanel, BorderLayout.EAST);
        
        return header;
    }
    
    private Color getStatusColor() {
        String status = aiProcessor.getNetworkStatus();
        if (status.contains("Fast")) return STATUS_ONLINE;
        if (status.contains("Slow")) return STATUS_SLOW;
        return STATUS_OFFLINE;
    }
    
    private JPanel createChatPanel() {
        JPanel panel = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, 
                    RenderingHints.VALUE_ANTIALIAS_ON);
                
                // Glassmorphism effect
                g2d.setColor(new Color(20, 20, 40, 200));
                g2d.fillRoundRect(0, 0, getWidth(), getHeight(), 20, 20);
                
                // Neon border glow
                g2d.setColor(new Color(0, 200, 255, 60));
                g2d.setStroke(new BasicStroke(2f));
                g2d.drawRoundRect(1, 1, getWidth() - 3, getHeight() - 3, 18, 18);
            }
        };
        panel.setOpaque(false);
        panel.setBorder(new EmptyBorder(20, 20, 20, 20));
        
        // Use JTextPane for styled text
        chatArea = new JTextPane();
        chatArea.setEditable(false);
        chatArea.setOpaque(false);
        chatArea.setFont(new Font("Consolas", Font.PLAIN, 15));
        chatArea.setForeground(TEXT_PRIMARY);
        chatArea.setCaretColor(NEON_CYAN);
        
        // Styled document for colored messages
        StyledDocument doc = chatArea.getStyledDocument();
        
        JScrollPane scrollPane = new JScrollPane(chatArea);
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.setBorder(null);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        
        // Custom scrollbar
        scrollPane.getVerticalScrollBar().setUI(new ModernScrollBarUI());
        
        panel.add(scrollPane, BorderLayout.CENTER);
        
        return panel;
    }
    
    private JPanel createInputPanel() {
        JPanel panel = new JPanel(new BorderLayout(15, 0));
        panel.setOpaque(false);
        panel.setBorder(new EmptyBorder(15, 0, 0, 0));
        
        // Input field with glassmorphism
        inputField = new JTextField() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, 
                    RenderingHints.VALUE_ANTIALIAS_ON);
                
                // Background with glow
                g2d.setColor(BG_INPUT);
                g2d.fillRoundRect(0, 0, getWidth(), getHeight(), 15, 15);
                
                // Border glow on focus
                if (hasFocus()) {
                    g2d.setColor(new Color(0, 200, 255, 100));
                    g2d.setStroke(new BasicStroke(3f));
                    g2d.drawRoundRect(1, 1, getWidth() - 3, getHeight() - 3, 14, 14);
                } else {
                    g2d.setColor(new Color(100, 100, 150, 80));
                    g2d.setStroke(new BasicStroke(2f));
                    g2d.drawRoundRect(1, 1, getWidth() - 3, getHeight() - 3, 14, 14);
                }
                
                super.paintComponent(g);
            }
        };
        inputField.setOpaque(false);
        inputField.setFont(new Font("Arial", Font.PLAIN, 18));
        inputField.setForeground(TEXT_PRIMARY);
        inputField.setCaretColor(NEON_CYAN);
        inputField.setBorder(new EmptyBorder(18, 25, 18, 25));
        inputField.setPreferredSize(new Dimension(0, 60));
        
        // Placeholder text behavior
        inputField.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                inputField.repaint();
            }
            @Override
            public void focusLost(FocusEvent e) {
                inputField.repaint();
            }
        });
        
        inputField.addActionListener(e -> sendMessage());
        
        // Buttons panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        buttonPanel.setOpaque(false);
        
        voiceButton = createGlowButton("🎤 Voice", NEON_PURPLE);
        voiceButton.setPreferredSize(new Dimension(110, 55));
        voiceButton.addActionListener(e -> startVoiceInput());
        
        sendButton = createGlowButton("Send →", NEON_CYAN);
        sendButton.setPreferredSize(new Dimension(110, 55));
        sendButton.addActionListener(e -> sendMessage());
        
        buttonPanel.add(voiceButton);
        buttonPanel.add(sendButton);
        
        panel.add(inputField, BorderLayout.CENTER);
        panel.add(buttonPanel, BorderLayout.EAST);
        
        return panel;
    }
    
    private JButton createGlowButton(String text, Color glowColor) {
        JButton button = new JButton(text) {
            private boolean isHovered = false;
            
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, 
                    RenderingHints.VALUE_ANTIALIAS_ON);
                
                // Glow effect on hover
                if (isHovered) {
                    g2d.setColor(new Color(glowColor.getRed(), glowColor.getGreen(), 
                        glowColor.getBlue(), 60));
                    g2d.fillRoundRect(-5, -5, getWidth() + 10, getHeight() + 10, 20, 20);
                }
                
                // Button background with gradient
                GradientPaint gradient = new GradientPaint(
                    0, 0, new Color(glowColor.getRed(), glowColor.getGreen(), 
                        glowColor.getBlue(), isHovered ? 180 : 120),
                    0, getHeight(), new Color(glowColor.getRed()/2, glowColor.getGreen()/2, 
                        glowColor.getBlue()/2, isHovered ? 180 : 120)
                );
                g2d.setPaint(gradient);
                g2d.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                
                // Border
                g2d.setColor(glowColor);
                g2d.setStroke(new BasicStroke(2f));
                g2d.drawRoundRect(1, 1, getWidth() - 3, getHeight() - 3, 11, 11);
                
                // Text
                g2d.setColor(Color.WHITE);
                g2d.setFont(getFont());
                FontMetrics fm = g2d.getFontMetrics();
                int x = (getWidth() - fm.stringWidth(getText())) / 2;
                int y = (getHeight() + fm.getAscent() - fm.getDescent()) / 2;
                g2d.drawString(getText(), x, y);
            }
            
            {
                setOpaque(false);
                setContentAreaFilled(false);
                setBorderPainted(false);
                setFocusPainted(false);
                setFont(new Font("Arial", Font.BOLD, 14));
                setCursor(new Cursor(Cursor.HAND_CURSOR));
                
                addMouseListener(new MouseAdapter() {
                    @Override
                    public void mouseEntered(MouseEvent e) {
                        isHovered = true;
                        repaint();
                    }
                    @Override
                    public void mouseExited(MouseEvent e) {
                        isHovered = false;
                        repaint();
                    }
                });
            }
        };
        
        return button;
    }
    
    private JPanel createStatusBar() {
        JPanel panel = new JPanel(new BorderLayout(10, 0)) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;
                g2d.setColor(new Color(15, 15, 25, 200));
                g2d.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
            }
        };
        panel.setOpaque(false);
        panel.setBorder(new EmptyBorder(12, 18, 12, 18));
        
        statusLabel = new JLabel("● Ready");
        statusLabel.setFont(new Font("Arial", Font.PLAIN, 12));
        statusLabel.setForeground(STATUS_ONLINE);
        
        audioLevelBar = new JProgressBar(0, 100);
        audioLevelBar.setPreferredSize(new Dimension(120, 8));
        audioLevelBar.setForeground(NEON_CYAN);
        audioLevelBar.setBackground(new Color(30, 30, 50));
        audioLevelBar.setBorderPainted(false);
        audioLevelBar.setVisible(false);
        
        JLabel timeLabel = new JLabel();
        timeLabel.setFont(new Font("Consolas", Font.PLAIN, 12));
        timeLabel.setForeground(TEXT_SECONDARY);
        
        Timer timer = new Timer(1000, e -> {
            LocalDateTime now = LocalDateTime.now();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss");
            timeLabel.setText(now.format(formatter));
        });
        timer.start();
        
        JPanel leftPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 0));
        leftPanel.setOpaque(false);
        leftPanel.add(statusLabel);
        leftPanel.add(audioLevelBar);
        
        panel.add(leftPanel, BorderLayout.WEST);
        panel.add(timeLabel, BorderLayout.EAST);
        
        return panel;
    }
    
    private void addWelcomeMessage() {
        appendMessage("I.R.I.S", "Welcome! I am I.R.I.S, your Intelligent Responsive Integrated System.", NEON_CYAN);
        appendMessage("SYSTEM", "Type a command or question below • Press Ctrl+Space for voice input", TEXT_SECONDARY);
    }
    
    private void sendMessage() {
        String message = inputField.getText().trim();
        if (message.isEmpty()) return;
        
        appendMessage("You", message, NEON_PURPLE);
        inputField.setText("");
        
        updateStatus("Processing...", STATUS_SLOW);
        
        SwingWorker<String, Void> worker = new SwingWorker<>() {
            @Override
            protected String doInBackground() {
                return commandHandler.processCommand(message);
            }
            
            @Override
            protected void done() {
                try {
                    String response = get();
                    
                    if (response.equals("exit")) {
                        appendMessage("I.R.I.S", "Goodbye! Shutting down...", NEON_CYAN);
                        speakResponse("Goodbye! Shutting down.");
                        updateStatus("Shutting down...", STATUS_OFFLINE);
                        Timer timer = new Timer(2000, e -> System.exit(0));
                        timer.setRepeats(false);
                        timer.start();
                    } else {
                        appendMessage("I.R.I.S", response, NEON_CYAN);
                        // Speak the response (truncate if too long)
                        speakResponse(response);
                        updateStatus("Ready", STATUS_ONLINE);
                    }
                } catch (Exception e) {
                    appendMessage("ERROR", "Failed: " + e.getMessage(), STATUS_OFFLINE);
                    updateStatus("Error", STATUS_OFFLINE);
                }
            }
        };
        
        worker.execute();
    }
    
    /**
     * Speak a response using text-to-speech
     */
    private void speakResponse(String response) {
        // Clean up the response for speech (remove emojis and special characters)
        String cleanResponse = response
            .replaceAll("[\\p{So}\\p{Cn}]", "") // Remove emojis and symbols
            .replaceAll("\\n+", ". ")           // Replace newlines with periods
            .replaceAll("\\s+", " ")            // Normalize whitespace
            .replaceAll("[=\\-_]{3,}", "")      // Remove separator lines
            .trim();
        
        // Truncate if too long (only speak first 200 characters for long responses)
        if (cleanResponse.length() > 200) {
            int cutPoint = cleanResponse.lastIndexOf('.', 200);
            if (cutPoint > 50) {
                cleanResponse = cleanResponse.substring(0, cutPoint + 1);
            } else {
                cleanResponse = cleanResponse.substring(0, 200) + "...";
            }
        }
        
        // Speak if there's meaningful content
        if (cleanResponse.length() > 5) {
            tts.speak(cleanResponse);
        }
    }
    
    private void appendMessage(String sender, String message, Color color) {
        SwingUtilities.invokeLater(() -> {
            StyledDocument doc = chatArea.getStyledDocument();
            
            // Create styles
            Style timeStyle = chatArea.addStyle("time", null);
            StyleConstants.setForeground(timeStyle, TEXT_SECONDARY);
            StyleConstants.setFontSize(timeStyle, 12);
            
            Style senderStyle = chatArea.addStyle("sender", null);
            StyleConstants.setForeground(senderStyle, color);
            StyleConstants.setBold(senderStyle, true);
            StyleConstants.setFontSize(senderStyle, 14);
            
            Style messageStyle = chatArea.addStyle("message", null);
            StyleConstants.setForeground(messageStyle, TEXT_PRIMARY);
            StyleConstants.setFontSize(messageStyle, 14);
            
            try {
                LocalDateTime now = LocalDateTime.now();
                String timestamp = now.format(DateTimeFormatter.ofPattern("HH:mm"));
                
                doc.insertString(doc.getLength(), "[" + timestamp + "] ", timeStyle);
                doc.insertString(doc.getLength(), sender + ": ", senderStyle);
                doc.insertString(doc.getLength(), message + "\n\n", messageStyle);
                
                chatArea.setCaretPosition(doc.getLength());
            } catch (BadLocationException e) {
                e.printStackTrace();
            }
        });
    }
    
    private void updateStatus(String status, Color color) {
        SwingUtilities.invokeLater(() -> {
            statusLabel.setText("● " + status);
            statusLabel.setForeground(color);
        });
    }
    
    private void setupKeyboardShortcuts() {
        KeyStroke ctrlSpace = KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, InputEvent.CTRL_DOWN_MASK);
        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(ctrlSpace, "voiceInput");
        getRootPane().getActionMap().put("voiceInput", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (!isListening) {
                    startVoiceInput();
                }
            }
        });
    }
    
    private void startVoiceInput() {
        if (isListening) return;
        
        isListening = true;
        voiceButton.setText("🔴 Listening...");
        audioLevelBar.setVisible(true);
        updateStatus("Listening (10s)...", STATUS_SLOW);
        
        SwingWorker<String, Float> worker = new SwingWorker<>() {
            @Override
            protected String doInBackground() {
                return speechRecognizer.listenWithCallback(10, new SpeechRecognizer.AudioLevelCallback() {
                    @Override
                    public void onAudioLevel(float level) {
                        publish(level);
                    }
                    @Override
                    public void onTranscript(String text) {}
                });
            }
            
            @Override
            protected void process(java.util.List<Float> levels) {
                if (!levels.isEmpty()) {
                    float level = levels.get(levels.size() - 1);
                    audioLevelBar.setValue((int)(level * 100));
                }
            }
            
            @Override
            protected void done() {
                try {
                    String transcript = get();
                    voiceButton.setText("🎤 Voice");
                    audioLevelBar.setVisible(false);
                    audioLevelBar.setValue(0);
                    isListening = false;
                    
                    if (transcript != null && !transcript.isEmpty()) {
                        inputField.setText(transcript);
                        updateStatus("Voice received", STATUS_ONLINE);
                        sendMessage();
                    } else {
                        updateStatus("No speech detected", STATUS_SLOW);
                        appendMessage("SYSTEM", "No speech detected. Try again.", STATUS_SLOW);
                    }
                } catch (Exception e) {
                    voiceButton.setText("🎤 Voice");
                    audioLevelBar.setVisible(false);
                    isListening = false;
                    updateStatus("Voice error", STATUS_OFFLINE);
                }
            }
        };
        
        worker.execute();
    }
    
    private void switchToGrok() {
        aiProcessor.setManualMode(true, "grok");
        appendMessage("SYSTEM", "🎯 Switched to GROK (primary AI)", new Color(255, 100, 50));
        updateAIButtonStates();
    }
    
    private void switchToGemini() {
        aiProcessor.setManualMode(true, "gemini");
        appendMessage("SYSTEM", "🌐 Switched to GEMINI (secondary AI)", NEON_BLUE);
        updateAIButtonStates();
    }
    
    private void switchToOllama() {
        aiProcessor.setManualMode(true, "ollama");
        appendMessage("SYSTEM", "🤖 Switched to OLLAMA (offline AI)", new Color(50, 200, 100));
        updateAIButtonStates();
    }
    
    private void updateAIButtonStates() {
        String mode = aiProcessor.getCurrentMode();
        SwingUtilities.invokeLater(() -> {
            // Reset all buttons
            grokButton.setText("🎯 Grok");
            geminiButton.setText("🌐 Gemini");
            ollamaButton.setText("🤖 Ollama");
            
            // Highlight the active one
            if (mode.equals("grok")) {
                grokButton.setText("✓ Grok");
            } else if (mode.equals("gemini")) {
                geminiButton.setText("✓ Gemini");
            } else if (mode.equals("ollama")) {
                ollamaButton.setText("✓ Ollama");
            }
        });
    }
    
    private void startNetworkStatusUpdater() {
        Timer timer = new Timer(30000, e -> {
            aiProcessor.refreshNetworkStatus();
            SwingUtilities.invokeLater(() -> {
                networkStatusLabel.setText(aiProcessor.getNetworkStatus());
                statusIndicatorPanel.repaint();
                updateAIButtonStates();
            });
        });
        timer.start();
    }
    
    // Custom scrollbar UI
    private static class ModernScrollBarUI extends javax.swing.plaf.basic.BasicScrollBarUI {
        @Override
        protected void configureScrollBarColors() {
            this.thumbColor = new Color(80, 80, 120);
            this.trackColor = new Color(20, 20, 35);
        }
        
        @Override
        protected JButton createDecreaseButton(int orientation) {
            return createZeroButton();
        }
        
        @Override
        protected JButton createIncreaseButton(int orientation) {
            return createZeroButton();
        }
        
        private JButton createZeroButton() {
            JButton button = new JButton();
            button.setPreferredSize(new Dimension(0, 0));
            return button;
        }
        
        @Override
        protected void paintThumb(Graphics g, JComponent c, Rectangle thumbBounds) {
            Graphics2D g2d = (Graphics2D) g;
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, 
                RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.setColor(thumbColor);
            g2d.fillRoundRect(thumbBounds.x + 2, thumbBounds.y, 
                thumbBounds.width - 4, thumbBounds.height, 8, 8);
        }
    }
    
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JarvisGUI gui = new JarvisGUI();
            gui.setVisible(true);
        });
    }
}
