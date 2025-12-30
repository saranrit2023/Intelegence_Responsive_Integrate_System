package com.jarvis.gui;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Animated background with floating particles (Gemini-style)
 */
public class AnimatedBackground extends JPanel {
    private List<Particle> particles;
    private Timer animationTimer;
    private static final int PARTICLE_COUNT = 80; // Reduced for better performance
    
    public AnimatedBackground() {
        setOpaque(false);
        particles = new ArrayList<>();
        initializeParticles();
        startAnimation();
    }
    
    private void initializeParticles() {
        Random random = new Random();
        
        for (int i = 0; i < PARTICLE_COUNT; i++) {
            int x = random.nextInt(getWidth() > 0 ? getWidth() : 800);
            int y = random.nextInt(getHeight() > 0 ? getHeight() : 600);
            int size = random.nextInt(3) + 1; // 1-4px (smaller particles)
            float speed = random.nextFloat() * 0.8f + 0.3f; // 0.3-1.1 (slightly faster)
            float angle = 270 + (random.nextFloat() * 40 - 20); // Mostly upward (250-290 degrees)
            
            // Random color - green/emerald tones (JARVIS theme)
            Color[] colors = {
                new Color(16, 185, 129, 80),    // Emerald-500 (#10B981)
                new Color(52, 211, 153, 100),   // Emerald-400 (#34D399)
                new Color(5, 150, 105, 90),     // Emerald-600 (#059669)
                new Color(4, 120, 87, 70),      // Emerald-700 (#047857)
                new Color(110, 231, 183, 110)   // Emerald-300 (#6EE7B7)
            };
            Color color = colors[random.nextInt(colors.length)];
            
            particles.add(new Particle(x, y, size, speed, angle, color));
        }
    }
    
    private void startAnimation() {
        animationTimer = new Timer(16, e -> { // ~60 FPS
            updateParticles();
            repaint();
        });
        animationTimer.start();
    }
    
    private void updateParticles() {
        int width = getWidth();
        int height = getHeight();
        
        if (width == 0 || height == 0) return;
        
        for (Particle p : particles) {
            // Update position
            p.x += Math.cos(Math.toRadians(p.angle)) * p.speed;
            p.y += Math.sin(Math.toRadians(p.angle)) * p.speed;
            
            // Wrap around edges
            if (p.x < -p.size) p.x = width + p.size;
            if (p.x > width + p.size) p.x = -p.size;
            if (p.y < -p.size) p.y = height + p.size;
            if (p.y > height + p.size) p.y = -p.size;
            
            // Slight angle variation for organic movement
            p.angle += (Math.random() - 0.5) * 2;
        }
    }
    
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        
        // Enable anti-aliasing
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        // Draw particles
        for (Particle p : particles) {
            // Glow effect
            float[] fractions = {0.0f, 0.5f, 1.0f};
            Color[] colors = {
                new Color(p.color.getRed(), p.color.getGreen(), p.color.getBlue(), 0),
                new Color(p.color.getRed(), p.color.getGreen(), p.color.getBlue(), p.color.getAlpha()),
                new Color(p.color.getRed(), p.color.getGreen(), p.color.getBlue(), 0)
            };
            
            RadialGradientPaint gradient = new RadialGradientPaint(
                (float) p.x, (float) p.y, p.size * 2,
                fractions, colors
            );
            
            g2d.setPaint(gradient);
            g2d.fill(new Ellipse2D.Double(
                p.x - p.size * 2,
                p.y - p.size * 2,
                p.size * 4,
                p.size * 4
            ));
            
            // Core circle
            g2d.setColor(p.color);
            g2d.fill(new Ellipse2D.Double(
                p.x - p.size / 2.0,
                p.y - p.size / 2.0,
                p.size,
                p.size
            ));
        }
        
        // Draw connections between nearby particles
        g2d.setColor(new Color(16, 185, 129, 30)); // Green connections
        for (int i = 0; i < particles.size(); i++) {
            for (int j = i + 1; j < particles.size(); j++) {
                Particle p1 = particles.get(i);
                Particle p2 = particles.get(j);
                
                double distance = Math.sqrt(
                    Math.pow(p1.x - p2.x, 2) + Math.pow(p1.y - p2.y, 2)
                );
                
                if (distance < 150) {
                    int alpha = (int) (30 * (1 - distance / 150));
                    g2d.setColor(new Color(16, 185, 129, alpha)); // Green with varying alpha
                    g2d.drawLine((int) p1.x, (int) p1.y, (int) p2.x, (int) p2.y);
                }
            }
        }
    }
    
    public void stopAnimation() {
        if (animationTimer != null) {
            animationTimer.stop();
        }
    }
    
    private static class Particle {
        double x, y;
        int size;
        float speed;
        float angle;
        Color color;
        
        Particle(double x, double y, int size, float speed, float angle, Color color) {
            this.x = x;
            this.y = y;
            this.size = size;
            this.speed = speed;
            this.angle = angle;
            this.color = color;
        }
    }
}
