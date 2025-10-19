package ru.usernamedrew.ui;

import ru.usernamedrew.lsystem.LSystem;
import ru.usernamedrew.models.Point;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Path2D;
import java.util.List;

//Что делает:
//
//Получает точки от LSystem
//Автоматически подгоняет размер под окно
//Рисует линии между точками

public class LSystemFractalPanel extends JPanel {
    private LSystem lsystem;
    private double scale = 1.0;

    public LSystemFractalPanel() {
        setBackground(Color.WHITE);
    }

    public void setLSystem(LSystem lsystem) {
        this.lsystem = lsystem;
        repaint();
    }

    public void setScale(double scale) {
        this.scale = scale;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        if (lsystem == null || lsystem.getPoints().isEmpty()) {
            return;
        }

        List<Point> points = lsystem.getPoints();

        // Находим границы фигуры
        double minX = Double.MAX_VALUE, minY = Double.MAX_VALUE;
        double maxX = Double.MIN_VALUE, maxY = Double.MIN_VALUE;

        for (Point p : points) {
            minX = Math.min(minX, p.x);
            minY = Math.min(minY, p.y);
            maxX = Math.max(maxX, p.x);
            maxY = Math.max(maxY, p.y);
        }

        // Вычисляем размеры фигуры и панели
        double figureWidth = maxX - minX;
        double figureHeight = maxY - minY;
        double panelWidth = getWidth();
        double panelHeight = getHeight();

        // Автоматическое масштабирование + ручной масштаб
        double autoScaleX = panelWidth * 0.8 / figureWidth;  // 80% ширины панели
        double autoScaleY = panelHeight * 0.8 / figureHeight; // 80% высоты панели

        // Автоматическое масштабирование: "Впиши фигуру в 80% окна"
        // double autoScale = Math.min(ширинаОкна/ширинаФигуры, высотаОкна/высотаФигуры);
        double autoScale = Math.min(autoScaleX, autoScaleY);

        // Комбинируем автоматическое и ручное масштабирование
        // Ручное масштабирование: "Умножь на коэффициент пользователя"
//        double finalScale = autoScale * ручнойМасштаб;
        double finalScale = autoScale * scale;

        // Центры
        double figureCenterX = (minX + maxX) / 2;
        double figureCenterY = (minY + maxY) / 2;
        double panelCenterX = panelWidth / 2;
        double panelCenterY = panelHeight / 2;

        // Создаем путь
        Path2D path = new Path2D.Double();
        boolean firstPoint = true;

        g2d.setColor(Color.BLUE);
        g2d.setStroke(new BasicStroke(1.5f));

        for (int i = 0; i < points.size(); i += 2) {
            if (i + 1 < points.size()) {
                Point p1 = points.get(i);
                Point p2 = points.get(i + 1);

                // Преобразуем координаты с учетом масштаба и центрирования
                double x1 = panelCenterX + (p1.x - figureCenterX) * finalScale;
                double y1 = panelCenterY + (p1.y - figureCenterY) * finalScale;
                double x2 = panelCenterX + (p2.x - figureCenterX) * finalScale;
                double y2 = panelCenterY + (p2.y - figureCenterY) * finalScale;

                if (firstPoint) {
                    path.moveTo(x1, y1);
                    firstPoint = false;
                }
                path.lineTo(x2, y2);
            }
        }

        g2d.draw(path);

        // Отладочная информация (можно убрать)
        if (scale < 0.3) {
            g2d.setColor(Color.RED);
            g2d.setFont(new Font("Arial", Font.PLAIN, 12));
            g2d.drawString("Масштаб: " + String.format("%.2f", scale) +
                    " | Авто-масштаб: " + String.format("%.2f", autoScale), 10, 20);
        }
    }
}