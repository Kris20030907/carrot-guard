package com.ktpro.carrotguard;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import java.awt.Dimension;

public final class Main {
    private Main() {
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Carrot Guard");
            GamePanel panel = new GamePanel();
            panel.setPreferredSize(new Dimension(GamePanel.WIDTH, GamePanel.HEIGHT));

            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setResizable(false);
            frame.setContentPane(panel);
            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);

            panel.start();
        });
    }
}

