// When loading GIFs, use proper scaling:
ImageIcon originalIcon = new ImageIcon(gifPath);
Image scaledImage = originalIcon.getImage().getScaledInstance(150, 150, Image.SCALE_SMOOTH); // Adjust size as needed
ImageIcon scaledIcon = new ImageIcon(scaledImage);

// For labels displaying GIFs:
JLabel gifLabel = new JLabel(scaledIcon);
gifLabel.setPreferredSize(new Dimension(150, 150));
