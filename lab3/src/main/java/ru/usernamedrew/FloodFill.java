package ru.usernamedrew;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;

public class FloodFill extends JFrame {
    private BufferedImage canvas;
    private JPanel canvasPanel;
    private Color fillColor = Color.RED;
    private Color borderColor = Color.BLACK;
    private boolean drawingBorder = false;
    private Point lastPoint;

    public FloodFill() {
        setTitle("Flood Fill Algorithm");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(800, 600);
        setLocationRelativeTo(null);

        initializeCanvas();
        setupUI();
    }

    private void initializeCanvas() {
        canvas = new BufferedImage(800, 600, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = canvas.createGraphics();
        g2d.setColor(Color.WHITE);
        g2d.fillRect(0, 0, 800, 600);
        g2d.dispose();
    }

    private void setupUI() {
        setLayout(new BorderLayout());

        // Панель для кнопок
        JPanel controlPanel = new JPanel();

        JButton borderBtn = new JButton("Рисовать границу");
        JButton fillBtn = new JButton("Залить цветом");
        JButton patternFillBtn = new JButton("Залить рисунком");
        JButton clearBtn = new JButton("Очистить");

        borderBtn.addActionListener(e -> drawingBorder = true);
        fillBtn.addActionListener(e -> drawingBorder = false);
        clearBtn.addActionListener(e -> {
            initializeCanvas();
            canvasPanel.repaint();
        });

        controlPanel.add(borderBtn);
        controlPanel.add(fillBtn);
        controlPanel.add(patternFillBtn);
        controlPanel.add(clearBtn);

        // Панель для отображения canvas
        canvasPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                g.drawImage(canvas, 0, 0, null);
            }
        };

        canvasPanel.setPreferredSize(new Dimension(800, 600));
        canvasPanel.setBackground(Color.WHITE);

        canvasPanel.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (drawingBorder) {
                    lastPoint = e.getPoint();
                } else {
                    // Заливка
                    floodFill(e.getX(), e.getY(), fillColor.getRGB());
                    canvasPanel.repaint();
                }
            }
        });

        canvasPanel.addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                if (drawingBorder && lastPoint != null) {
                    Graphics2D g2d = canvas.createGraphics();
                    g2d.setColor(borderColor);
                    g2d.setStroke(new BasicStroke(2));
                    g2d.drawLine(lastPoint.x, lastPoint.y, e.getX(), e.getY());
                    g2d.dispose();
                    lastPoint = e.getPoint();
                    canvasPanel.repaint();
                }
            }
        });

        add(controlPanel, BorderLayout.NORTH);
        add(canvasPanel, BorderLayout.CENTER);
    }

    // 1а) Рекурсивный алгоритм заливки на основе серий пикселов
    private void floodFill(int x, int y, int newColor) {
        int targetColor = canvas.getRGB(x, y);
        if (targetColor == newColor) return;

        floodFillScanline(x, y, targetColor, newColor);
    }

    private void floodFillScanline(int x, int y, int targetColor, int newColor) {
        if (x < 0 || x >= canvas.getWidth() || y < 0 || y >= canvas.getHeight())
            return;

        if (canvas.getRGB(x, y) != targetColor)
            return;

        // Находим левую границу линии
        int left = x;
        while (left > 0 && canvas.getRGB(left - 1, y) == targetColor) {
            left--;
        }

        // Находим правую границу линии
        int right = x;
        while (right < canvas.getWidth() - 1 && canvas.getRGB(right + 1, y) == targetColor) {
            right++;
        }

        // Закрашиваем всю линию
        for (int i = left; i <= right; i++) {
            canvas.setRGB(i, y, newColor);
        }

        // Рекурсивно обрабатываем строки выше и ниже
        for (int i = left; i <= right; i++) {
            if (y > 0 && canvas.getRGB(i, y - 1) == targetColor) {
                floodFillScanline(i, y - 1, targetColor, newColor);
            }
            if (y < canvas.getHeight() - 1 && canvas.getRGB(i, y + 1) == targetColor) {
                floodFillScanline(i, y + 1, targetColor, newColor);
            }
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new FloodFill().setVisible(true);
        });
    }
}