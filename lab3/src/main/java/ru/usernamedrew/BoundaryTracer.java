package ru.usernamedrew;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.*;

public class BoundaryTracer extends JPanel {
    private BufferedImage image;
    private final ArrayList<Point> points = new ArrayList<>();
    private final int size = 600;

    public BoundaryTracer() {
        createImage();
        findBoundary();
        setPreferredSize(new Dimension(size, size));
    }

    // Создаем тестовое изображение
    private void createImage() {
        image = new BufferedImage(size, size, BufferedImage.TYPE_INT_RGB);
        Graphics g = image.getGraphics();

        g.setColor(Color.WHITE);
        g.fillRect(0, 0, size, size);

        // Рисуем фигуру с красной границей
        g.setColor(Color.BLUE);
        g.fillRect(150, 150, 300, 300);

        g.setColor(Color.RED);
        ((Graphics2D)g).setStroke(new BasicStroke(3));
        g.drawRect(150, 150, 300, 300);
        g.drawLine(300, 450, 350, 500);
        g.drawLine(350, 500, 400, 450);

        g.dispose();
    }

    // Находим границу
    private void findBoundary() {
        Point start = findStart();
        if (start == null) return;

        Point current = start;
        Point prev = new Point(start.x - 1, start.y);

        do {
            points.add(new Point(current.x, current.y));
            Point next = findNext(current, prev);
            prev = current;
            current = next;
        } while (!current.equals(start) && points.size() < 1000);

        drawBoundary();
    }

    // Ищем начальную точку
    private Point findStart() {
        for (int y = 0; y < size; y++)
            for (int x = 0; x < size; x++)
                if (isRed(image.getRGB(x, y)))
                    return new Point(x, y);
        return null;
    }

    // Проверка красного цвета
    private boolean isRed(int rgb) {
        Color c = new Color(rgb);
        return c.getRed() > 200 && c.getGreen() < 50 && c.getBlue() < 50;
    }

    // Ищем следующую точку границы
    private Point findNext(Point curr, Point prev) {
        int[][] dirs = {{1,0}, {1,1}, {0,1}, {-1,1}, {-1,0}, {-1,-1}, {0,-1}, {1,-1}};
        int startDir = getDir(prev, curr);

        for (int i = 0; i < 8; i++) {
            int dir = (startDir + i) % 8;
            int nx = curr.x + dirs[dir][0], ny = curr.y + dirs[dir][1];
            if (nx >= 0 && nx < size && ny >= 0 && ny < size && isRed(image.getRGB(nx, ny)))
                return new Point(nx, ny);
        }
        return curr;
    }

    // Определяем направление
    private int getDir(Point from, Point to) {
        int dx = to.x - from.x, dy = to.y - from.y;
        if (dx == 1 && dy == 0) return 0;
        if (dx == 1 && dy == 1) return 1;
        if (dx == 0 && dy == 1) return 2;
        if (dx == -1 && dy == 1) return 3;
        if (dx == -1 && dy == 0) return 4;
        if (dx == -1 && dy == -1) return 5;
        if (dx == 0 && dy == -1) return 6;
        return 7;
    }

    // Рисуем найденную границу
    private void drawBoundary() {
        Graphics g = image.getGraphics();
        g.setColor(Color.GREEN);
        ((Graphics2D)g).setStroke(new BasicStroke(2));

        for (int i = 0; i < points.size(); i++) {
            Point p = points.get(i);
            if (i < points.size() - 1) {
                Point next = points.get(i + 1);
                g.drawLine(p.x, p.y, next.x, next.y);
            }
        }

        if (!points.isEmpty()) {
            Point first = points.get(0), last = points.get(points.size() - 1);
            g.drawLine(last.x, last.y, first.x, first.y);
        }

        g.dispose();
    }

    @Override
    protected void paintComponent(Graphics g) {
        g.drawImage(image, 0, 0, null);
        g.setColor(Color.BLACK);
        g.drawString("Точек границы: " + points.size(), 10, 20);
    }

    public static void main(String[] args) {
        JFrame frame = new JFrame("Boundary Tracer");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.add(new BoundaryTracer());
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }
}
