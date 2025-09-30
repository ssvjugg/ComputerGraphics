package ru.usernamedrew;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

public class BarycentricTriangle extends JPanel {
    private final BufferedImage image;
    private final int width = 600;
    private final int height = 600;

    // Вершины треугольника с разными цветами
    private final Point p1 = new Point(300, 100);
    private final Point p2 = new Point(100, 500);
    private final Point p3 = new Point(500, 500);

    private final Color color1 = Color.RED;
    private final Color color2 = Color.GREEN;
    private final Color color3 = Color.BLUE;

    public BarycentricTriangle() {
        image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        rasterizeTriangle();
        setPreferredSize(new Dimension(width, height));
    }

    // Растеризация треугольника через барицентрические координаты
    private void rasterizeTriangle() {
        // 1. Находим ограничивающий прямоугольник треугольника
        int minX = Math.min(p1.x, Math.min(p2.x, p3.x));
        int maxX = Math.max(p1.x, Math.max(p2.x, p3.x));
        int minY = Math.min(p1.y, Math.min(p2.y, p3.y));
        int maxY = Math.max(p1.y, Math.max(p2.y, p3.y));

        // Вычисляем общую площадь треугольника для барицентрических координат
        double totalArea = calculateTriangleArea(p1, p2, p3);

        // 2. Проходим по всем пикселям в ограничивающем прямоугольнике
        for (int y = minY; y <= maxY; y++) {
            for (int x = minX; x <= maxX; x++) {
                Point p = new Point(x, y);

                // 3. Вычисляем три барицентрические координаты
                double alpha = calculateTriangleArea(p, p2, p3) / totalArea;
                double beta = calculateTriangleArea(p1, p, p3) / totalArea;
                double gamma = calculateTriangleArea(p1, p2, p) / totalArea;

                // 4. Проверяем, находится ли пиксель внутри треугольника
                // (все барицентрические координаты неотрицательны)
                if (alpha >= 0 && beta >= 0 && gamma >= 0 &&
                        Math.abs(alpha + beta + gamma - 1.0) < 0.0001) {

                    // 5. Интерполируем цвет с помощью барицентрических координат
                    Color pixelColor = interpolateColor(alpha, beta, gamma);

                    // Рисуем пиксель
                    if (x >= 0 && x < width && y >= 0 && y < height) {
                        image.setRGB(x, y, pixelColor.getRGB());
                    }
                }
            }
        }
    }

    // Вычисление площади треугольника по формуле через координаты
    private double calculateTriangleArea(Point a, Point b, Point c) {
        return Math.abs(
                (a.x * (b.y - c.y) +
                        b.x * (c.y - a.y) +
                        c.x * (a.y - b.y)) / 2.0
        );
    }

    // Интерполяция цвета с помощью барицентрических координат
    private Color interpolateColor(double alpha, double beta, double gamma) {
        // Каждый цветовой канал вычисляется как взвешенная сумма цветов вершин
        int red = (int)(alpha * color1.getRed() + beta * color2.getRed() + gamma * color3.getRed());
        int green = (int)(alpha * color1.getGreen() + beta * color2.getGreen() + gamma * color3.getGreen());
        int blue = (int)(alpha * color1.getBlue() + beta * color2.getBlue() + gamma * color3.getBlue());

        return new Color(red, green, blue);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        g.setColor(Color.WHITE);
        g.fillRect(0, 0, width, height);

        g.drawImage(image, 0, 0, this);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Barycentric Triangle Rasterization");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.add(new BarycentricTriangle());
            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}
