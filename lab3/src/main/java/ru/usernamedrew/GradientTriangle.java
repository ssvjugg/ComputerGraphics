package ru.usernamedrew;

import java.awt.*;
import java.awt.image.BufferedImage;
import javax.swing.*;

// 1. скомпилировать файл (javac GradientTriangle.java)
// 2. запустить файл (java GradientTriangle)

public class GradientTriangle extends JPanel {
    private final BufferedImage image;
    private final int width = 600;
    private final int height = 600;

    private final Point p1 = new Point(300, 100);
    private final Point p2 = new Point(100, 500);
    private final Point p3 = new Point(500, 500);

    private final Color color1 = Color.RED;
    private final Color color2 = Color.GREEN;
    private final Color color3 = Color.BLUE;

    // cоздает изображение 600x600 пикселей и запускает рисование треугольника
    public GradientTriangle() {
        image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        drawGradientTriangle();
        setPreferredSize(new Dimension(width, height));
    }

    // подготавливает холст и запускает основной алгоритм рисования
    private void drawGradientTriangle() {
        Graphics2D g2d = image.createGraphics();
        g2d.setColor(Color.WHITE);
        g2d.fillRect(0, 0, width, height);
        g2d.dispose();

        // сортируем вершины по Y
        Point[] points = {p1, p2, p3};
        java.util.Arrays.sort(points, (a, b) -> Integer.compare(a.y, b.y));

        // растеризуем с помощью построчного алгоритма
        rasterizeScanline(points[0], points[1], points[2]);
    }

    private void rasterizeScanline(Point top, Point mid, Point bottom) {
        // интерполируем цвета вдоль ребер
        for (int y = top.y; y <= bottom.y; y++) {
            int x1, x2;
            Color c1, c2;

            if (y < mid.y) {
                // верхняя часть треугольника
                x1 = interpolate(top.x, mid.x, top.y, mid.y, y);
                x2 = interpolate(top.x, bottom.x, top.y, bottom.y, y);
                c1 = interpolateColor(color1, color2, top.y, mid.y, y);
                c2 = interpolateColor(color1, color3, top.y, bottom.y, y);
            } else {
                // нижняя часть треугольника
                x1 = interpolate(mid.x, bottom.x, mid.y, bottom.y, y);
                x2 = interpolate(top.x, bottom.x, top.y, bottom.y, y);
                c1 = interpolateColor(color2, color3, mid.y, bottom.y, y);
                c2 = interpolateColor(color1, color3, top.y, bottom.y, y);
            }

            if (x1 > x2) {
                int tempX = x1;
                x1 = x2;
                x2 = tempX;
                Color tempC = c1;
                c1 = c2;
                c2 = tempC;
            }

            // заполняем горизонтальную линию
            for (int x = x1; x <= x2; x++) {
                Color color = interpolateColor(c1, c2, x1, x2, x);
                if (x >= 0 && x < width && y >= 0 && y < height) {
                    image.setRGB(x, y, color.getRGB());
                }
            }
        }
    }

    // линейная интерполяция (для нахождения x координаты)
    private int interpolate(int start, int end, int startY, int endY, int y) {
        if (startY == endY) return start;
        return start + (end - start) * (y - startY) / (endY - startY);
    }

    // интерполяция цвета
    private Color interpolateColor(Color start, Color end, int startPos, int endPos, int pos) {
        if (startPos == endPos) return start;

        float t = (float)(pos - startPos) / (endPos - startPos); // от 0 до 1 (прогресс)
        int r = (int)(start.getRed() + (end.getRed() - start.getRed()) * t);
        int g = (int)(start.getGreen() + (end.getGreen() - start.getGreen()) * t);
        int b = (int)(start.getBlue() + (end.getBlue() - start.getBlue()) * t);

        return new Color(r, g, b);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        g.drawImage(image, 0, 0, this);
    }

    public static void main(String[] args) {
        JFrame frame = new JFrame("Gradient Triangle");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.add(new GradientTriangle());
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }
}