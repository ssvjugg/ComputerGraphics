package ru.usernamedrew;

import javax.swing.*;
import java.awt.*;

public class App {
    public static void main( String[] args ) {
        SwingUtilities.invokeLater(App::createAndShowGUI);
    }

    private static void createAndShowGUI() {
        JFrame mainFrame = new JFrame("lab3");
        mainFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        mainFrame.setLayout(new BorderLayout());

        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new GridLayout(4, 1, 10, 10));
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JButton lineDrawingBtn = new JButton("Алгоритмы рисования линий");
        JButton triangleBtn = new JButton("Барцентрический треугольник");
        JButton boundaryTracerBtn = new JButton("Трассировка границ");
        JButton floodFillBtn = new JButton("Заливка областей");

        buttonPanel.add(lineDrawingBtn);
        buttonPanel.add(triangleBtn);
        buttonPanel.add(boundaryTracerBtn);
        buttonPanel.add(floodFillBtn);

        JLabel titleLabel = new JLabel("Выберите задание:", JLabel.CENTER);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 18));
        titleLabel.setBorder(BorderFactory.createEmptyBorder(20, 0, 20, 0));

        lineDrawingBtn.addActionListener(e -> openLineDrawing());

        triangleBtn.addActionListener(e -> openBarycentricTriangle());

        boundaryTracerBtn.addActionListener(e -> openBoundaryTracer());

        floodFillBtn.addActionListener(e -> openFloodFill());

        mainFrame.add(buttonPanel, BorderLayout.CENTER);
        mainFrame.add(titleLabel, BorderLayout.NORTH);

        mainFrame.pack();
        mainFrame.setLocationRelativeTo(null);
        mainFrame.setVisible(true);
    }

    private static void openLineDrawing() {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Алгоритмы рисования линий");
            DrawingLine.LineDrawerPanel panel = new DrawingLine.LineDrawerPanel(50, 20, 25, 10);
            panel.setPreferredSize(new Dimension(400, 300));

            // Панель для переключения алгоритмов
            JPanel controlPanel = new JPanel();
            JButton bresenhamBtn = new JButton("Алгоритм Брезенхема");
            JButton wuBtn = new JButton("Алгоритм Ву");

            bresenhamBtn.addActionListener(e -> panel.setAlgo(true));
            wuBtn.addActionListener(e -> panel.setAlgo(false));

            controlPanel.add(bresenhamBtn);
            controlPanel.add(wuBtn);

            frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            frame.setLayout(new BorderLayout());
            frame.add(controlPanel, BorderLayout.NORTH);
            frame.add(panel, BorderLayout.CENTER);
            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);

            panel.setAlgo(true); // По умолчанию показываем Брезенхема
        });
    }

    private static void openBarycentricTriangle() {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Барцентрический треугольник");
            frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            frame.add(new BarycentricTriangle());
            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }

    private static void openBoundaryTracer() {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Трассировка границ");
            frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            frame.add(new BoundaryTracer());
            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }

    private static void openFloodFill() {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new FloodFill();
            frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            frame.setVisible(true);
        });
    }
}

