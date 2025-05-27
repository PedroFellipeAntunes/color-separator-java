package Windows;

import ColorSeparator.Operations;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

public class ImageViewerGrid extends JDialog {
    private JPanel panel;
    private JPanel buttonPanel;
    private JButton saveButton;
    private JButton goBackButton;
    
    private final Operations operations;
    
    private boolean goBack = false;
    
    private final int MIN_WIDTH = 500;
    private final int MIN_HEIGHT = 500;
    
    public boolean wentBack() {
        return goBack;
    }
    
    public ImageViewerGrid(BufferedImage[] images, String filePath, Operations operations) {
        super((Frame) null, "Image Viewer Grid", true);
        this.operations = operations;
        
        if (images == null || images.length != 4) {
            throw new IllegalArgumentException("Exactly 4 images are required.");
        }
        
        panel = new JPanel(new GridLayout(2, 2, 5, 5));
        panel.setBackground(new Color(61, 56, 70));
        
        for (BufferedImage image : images) {
            panel.add(new ImagePanel(image));
        }
        
        buttonPanel = new JPanel();
        buttonPanel.setBackground(Color.BLACK);
        
        saveButton = new JButton("Save");
        goBackButton = new JButton("Go Back");
        
        setButtonsVisuals(saveButton);
        setButtonsVisuals(goBackButton);
        
        saveButton.addActionListener(e -> {
            operations.saveImages(images, filePath);
            goBack = true;
            dispose();
        });
        
        goBackButton.addActionListener(e -> {
            goBack = true;
            dispose();
        });
        
        buttonPanel.add(saveButton);
        buttonPanel.add(goBackButton);
        
        add(buttonPanel, BorderLayout.SOUTH);
        add(panel, BorderLayout.CENTER);
        
        adjustWindowSize(images);
        
        setResizable(false);
        setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);
        setVisible(true);
    }
    
    private void setButtonsVisuals(JButton button) {
        button.setBackground(Color.BLACK);
        button.setForeground(Color.WHITE);
        button.setBorder(BorderFactory.createLineBorder(Color.WHITE));
        button.setFocusPainted(false);
        button.setPreferredSize(new Dimension(100, 40));
    }
    
    private void adjustWindowSize(BufferedImage[] images) {
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        int maxWidth = (int) (screenSize.width * 0.85);
        int maxHeight = (int) (screenSize.height * 0.85);
        
        int totalWidth = 0;
        int totalHeight = 0;
        
        for (BufferedImage image : images) {
            totalWidth = Math.max(totalWidth, image.getWidth());
            totalHeight = Math.max(totalHeight, image.getHeight());
        }
        
        totalWidth *= 2; // 2 columns
        totalHeight *= 2; // 2 rows
        
        int finalWidth = Math.max(MIN_WIDTH, Math.min(maxWidth, totalWidth));
        int finalHeight = Math.max(MIN_HEIGHT, Math.min(maxHeight, totalHeight));
        
        setSize(finalWidth, finalHeight);
    }
    
    class ImagePanel extends JPanel {
        private BufferedImage image;
        
        public ImagePanel(BufferedImage image) {
            this.image = image;
            setBackground(new Color(61, 56, 70));
        }
        
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            double scaleX = (double) getWidth() / image.getWidth();
            double scaleY = (double) getHeight() / image.getHeight();
            
            double scale = Math.min(scaleX, scaleY);
            
            int newWidth = (int) (image.getWidth() * scale);
            int newHeight = (int) (image.getHeight() * scale);
            
            int x = (getWidth() - newWidth) / 2;
            int y = (getHeight() - newHeight) / 2;
            
            g.drawImage(image, x, y, newWidth, newHeight, this);
        }
    }
}