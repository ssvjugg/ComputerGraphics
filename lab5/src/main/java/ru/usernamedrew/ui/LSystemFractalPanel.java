package ru.usernamedrew.ui;

import ru.usernamedrew.lsystem.LSystem;
import ru.usernamedrew.models.LineSegment;
import ru.usernamedrew.models.Point;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Path2D;
import java.util.List;

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

        if (lsystem == null || lsystem.getSegments().isEmpty()) {
            return;
        }

        List<LineSegment> segments = lsystem.getSegments();

        // Находим границы фигуры
        double minX = Double.MAX_VALUE, minY = Double.MAX_VALUE;
        double maxX = Double.MIN_VALUE, maxY = Double.MIN_VALUE;

        for (LineSegment segment : segments) {
            minX = Math.min(minX, Math.min(segment.start.x, segment.end.x));
            minY = Math.min(minY, Math.min(segment.start.y, segment.end.y));
            maxX = Math.max(maxX, Math.max(segment.start.x, segment.end.x));
            maxY = Math.max(maxY, Math.max(segment.start.y, segment.end.y));
        }

        // Вычисляем размеры фигуры и панели
        double figureWidth = maxX - minX;
        double figureHeight = maxY - minY;
        double panelWidth = getWidth();
        double panelHeight = getHeight();

        // Автоматическое масштабирование + ручной масштаб
        double autoScaleX = panelWidth * 0.8 / figureWidth;
        double autoScaleY = panelHeight * 0.8 / figureHeight;
        double autoScale = Math.min(autoScaleX, autoScaleY);

        // Комбинируем автоматическое и ручное масштабирование
        double finalScale = autoScale * scale;

        // Центры
        double figureCenterX = (minX + maxX) / 2;
        double figureCenterY = (minY + maxY) / 2;
        double panelCenterX = panelWidth / 2;
        double panelCenterY = panelHeight / 2;

        // Рисуем каждый отрезок с его атрибутами
        for (LineSegment segment : segments) {
            // Преобразуем координаты
            double x1 = panelCenterX + (segment.start.x - figureCenterX) * finalScale;
            double y1 = panelCenterY + (segment.start.y - figureCenterY) * finalScale;
            double x2 = panelCenterX + (segment.end.x - figureCenterX) * finalScale;
            double y2 = panelCenterY + (segment.end.y - figureCenterY) * finalScale;

            // Устанавливаем толщину и цвет
            g2d.setColor(segment.color);
            g2d.setStroke(new BasicStroke((float) Math.max(0.5, segment.thickness * finalScale)));

            // Рисуем линию
            g2d.drawLine((int) x1, (int) y1, (int) x2, (int) y2);
        }
    }
}