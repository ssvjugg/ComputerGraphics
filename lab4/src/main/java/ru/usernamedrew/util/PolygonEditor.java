package ru.usernamedrew.util;

import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;
import ru.usernamedrew.model.Polygon;

import javax.swing.*;

public class PolygonEditor extends JFrame {
    private final List<Polygon> polygons;
    private Polygon currentPolygon;
    private JPanel drawingPanel;

    public PolygonEditor() {
        polygons = new ArrayList<>();
        currentPolygon = new Polygon();

        initializeUI();
    }

    private void initializeUI() {
        setTitle("Polygon Editor");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // Панель для рисования
        drawingPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                // Рисуем все полигоны
                for (Polygon polygon : polygons) {
                    if (polygon.getVertexCount() > 0) {
                        drawPolygon(g2d, polygon);
                    }
                }
            }
        };

        drawingPanel.setBackground(Color.WHITE);
        drawingPanel.setPreferredSize(new Dimension(1000, 800));
        drawingPanel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                handleMouseClick(e);
            }
        });

        // Панель управления
        JPanel controlPanel = new JPanel();
        JButton clearButton = new JButton("Очистить сцену");
        JButton newPolygonButton = new JButton("Новый полигон");

        clearButton.addActionListener(e -> clearScene());
        newPolygonButton.addActionListener(e -> startNewPolygon());

        controlPanel.add(newPolygonButton);
        controlPanel.add(clearButton);

        add(controlPanel, BorderLayout.NORTH);
        add(drawingPanel, BorderLayout.CENTER);

        pack();
        setLocationRelativeTo(null);
    }

    private void handleMouseClick(MouseEvent e) {
        Point2D.Double point = new Point2D.Double(e.getX(), e.getY());
        currentPolygon.addVertex(point);

        // Если это первая точка нового полигона, добавляем его в список
        if (currentPolygon.getVertexCount() == 1) {
            polygons.add(currentPolygon);
        }

        drawingPanel.repaint();
    }

    private void drawPolygon(Graphics2D g2d, Polygon polygon) {
        List<Point2D.Double> vertices = polygon.getVertices();
        g2d.setColor(polygon.getColor());

        // Рисуем вершины
        for (Point2D.Double vertex : vertices) {
            g2d.fillOval((int)vertex.x - 3, (int)vertex.y - 3, 6, 6);
        }

        // Рисуем рёбра (если есть хотя бы 2 вершины)
        if (vertices.size() >= 2) {
            for (int i = 0; i < vertices.size() - 1; i++) {
                Point2D.Double p1 = vertices.get(i);
                Point2D.Double p2 = vertices.get(i + 1);
                g2d.drawLine((int)p1.x, (int)p1.y, (int)p2.x, (int)p2.y);
            }
        }
    }

    private void clearScene() {
        polygons.clear();
        currentPolygon.clear();
        drawingPanel.repaint();
    }

    private void startNewPolygon() {
        currentPolygon = new Polygon();
    }
}
