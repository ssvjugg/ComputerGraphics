package ru.usernamedrew.util;

import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;
import ru.usernamedrew.model.Polygon;

import javax.swing.*;

public class PolygonEditor extends JFrame {
    private final List<Polygon> polygons;
    private Polygon currentPolygon;
    private JPanel drawingPanel;
    private JTextArea infoArea; // Добавление текстовой области для информации
    private Point2D.Double cursorPoint; // Добавление отслеживания позиции курсора
    private int selectedEdgeIndex; // Добавление индекса выбранного ребра

    public PolygonEditor() {
        polygons = new ArrayList<>();
        currentPolygon = new Polygon();

        // Начальные значения "неактивного состояния"
        cursorPoint = null;
        selectedEdgeIndex = -1;

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
                for (int i = 0; i < polygons.size(); i++) {
                    Polygon polygon = polygons.get(i);
                    if (polygon.getVertexCount() > 0) {
                        drawPolygon(g2d, polygon, i);
                    }
                }

                // Рисуем курсор и информацию о проверке
                if (cursorPoint != null) {
                    drawCursorInfo(g2d);
                }
            }
        };

        drawingPanel.setBackground(Color.WHITE);
        drawingPanel.setPreferredSize(new Dimension(1000, 600));

        // Обработчики мыши
        drawingPanel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getButton() == MouseEvent.BUTTON1) {
                    // Левая кнопка - добавление вершин
                    handleMouseClick(e);
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                cursorPoint = null;
                updateInfo();
                drawingPanel.repaint();
            }
        });

        // Динамическое отслеживание курсора

        // При каждом движении мыши обновляется cursorPoint
        // Автоматически вызывается updateInfo() для пересчёта
        // repaint() перерисовывает экран с новой информацией
        drawingPanel.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                cursorPoint = new Point2D.Double(e.getX(), e.getY());
                updateInfo();
                drawingPanel.repaint();
            }
        });

        // Панель информации
        infoArea = new JTextArea(15, 50);
        infoArea.setEditable(false);
        infoArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        JScrollPane scrollPane = new JScrollPane(infoArea);

        // Панель управления
        JPanel controlPanel = new JPanel();
        JButton clearButton = new JButton("Очистить сцену");
        JButton newPolygonButton = new JButton("Новый полигон");
        JButton selectEdgeButton = new JButton("Выбрать ребро для проверки");

        clearButton.addActionListener(e -> clearScene());
        newPolygonButton.addActionListener(e -> startNewPolygon());
        selectEdgeButton.addActionListener(e -> selectEdgeForTesting());

        controlPanel.add(newPolygonButton);
        controlPanel.add(clearButton);
        controlPanel.add(selectEdgeButton);

        add(controlPanel, BorderLayout.NORTH);
        add(drawingPanel, BorderLayout.CENTER);
        add(scrollPane, BorderLayout.SOUTH);

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

        updateInfo();
        drawingPanel.repaint();
    }

    private void drawPolygon(Graphics2D g2d, Polygon polygon, int polygonIndex) {
        List<Point2D.Double> vertices = polygon.getVertices();
        g2d.setColor(polygon.getColor());

        // Рисуем вершины с номерами
        for (int i = 0; i < vertices.size(); i++) {
            Point2D.Double vertex = vertices.get(i);
            g2d.fillOval((int)vertex.x - 3, (int)vertex.y - 3, 6, 6);
            g2d.setColor(Color.BLACK);
            g2d.drawString(String.valueOf(i + 1), (int)vertex.x + 5, (int)vertex.y - 5);
            g2d.setColor(polygon.getColor());
        }

        // Рисуем рёбра с номерами
        if (vertices.size() >= 2) {
            for (int i = 0; i < vertices.size(); i++) {
                Point2D.Double p1 = vertices.get(i);
                Point2D.Double p2 = vertices.get((i + 1) % vertices.size());

                // Рисуем линию ребра
                g2d.drawLine((int)p1.x, (int)p1.y, (int)p2.x, (int)p2.y);

                // Подписываем ребро
                Point2D.Double midPoint = new Point2D.Double(
                        (p1.x + p2.x) / 2,
                        (p1.y + p2.y) / 2
                );
                g2d.setColor(Color.BLUE);
                g2d.drawString("Ребро " + (i + 1), (int)midPoint.x + 5, (int)midPoint.y - 5);
                g2d.setColor(polygon.getColor());

                // Выделяем выбранное ребро
                if (selectedEdgeIndex == i && polygonIndex == polygons.size() - 1) {
                    g2d.setStroke(new BasicStroke(3));
                    g2d.setColor(Color.RED);
                    g2d.drawLine((int)p1.x, (int)p1.y, (int)p2.x, (int)p2.y);
                    g2d.setStroke(new BasicStroke(1));
                    g2d.setColor(polygon.getColor());
                }
            }
        }

        // Подписываем полигон (выпуклый/невыпуклый)
        if (vertices.size() >= 3) {
            String label = polygon.isConvex() ? "Выпуклый" : "Невыпуклый";
            Point2D.Double center = getPolygonCenter(vertices);
            g2d.setColor(Color.BLACK);
            g2d.drawString("Полигон " + (polygonIndex + 1) + " (" + label + ")",
                    (int)center.x, (int)center.y);
        }
    }

    // Рисует крестик вокруг текущей позиции курсора
    //Помогает точно позиционировать точку для проверки
    private void drawCursorInfo(Graphics2D g2d) {
        g2d.setColor(Color.GRAY);
        g2d.drawLine((int)cursorPoint.x - 10, (int)cursorPoint.y,
                (int)cursorPoint.x + 10, (int)cursorPoint.y);
        g2d.drawLine((int)cursorPoint.x, (int)cursorPoint.y - 10,
                (int)cursorPoint.x, (int)cursorPoint.y + 10);

        // Подписываем координаты
        g2d.setColor(Color.BLACK);
        g2d.drawString(String.format("(%.1f, %.1f)", cursorPoint.x, cursorPoint.y),
                (int)cursorPoint.x + 15, (int)cursorPoint.y - 10);
    }

    private Point2D.Double getPolygonCenter(List<Point2D.Double> vertices) {
        double centerX = 0, centerY = 0;
        for (Point2D.Double vertex : vertices) {
            centerX += vertex.x;
            centerY += vertex.y;
        }
        return new Point2D.Double(centerX / vertices.size(), centerY / vertices.size());
    }

    private void selectEdgeForTesting() {
        if (polygons.isEmpty() || currentPolygon.getVertexCount() < 2) {
            JOptionPane.showMessageDialog(this,
                    "Создайте полигон с хотя бы 2 вершинами",
                    "Внимание", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String input = JOptionPane.showInputDialog(this,
                "Введите номер ребра для проверки (1-" + currentPolygon.getVertexCount() + "):",
                "Выбор ребра", JOptionPane.QUESTION_MESSAGE);

        if (input != null) {
            try {
                int edgeNum = Integer.parseInt(input);
                if (edgeNum >= 1 && edgeNum <= currentPolygon.getVertexCount()) {
                    selectedEdgeIndex = edgeNum - 1;
                    updateInfo();
                    drawingPanel.repaint();
                } else {
                    JOptionPane.showMessageDialog(this,
                            "Некорректный номер ребра", "Ошибка", JOptionPane.ERROR_MESSAGE);
                }
            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(this,
                        "Введите число", "Ошибка", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void updateInfo() {
        StringBuilder info = new StringBuilder();

        if (cursorPoint != null) {
            info.append("ПОЛОЖЕНИЕ КУРСОРА: (").append(String.format("%.1f", cursorPoint.x))
                    .append(", ").append(String.format("%.1f", cursorPoint.y)).append(")\n\n");
        }

        for (int i = 0; i < polygons.size(); i++) {
            Polygon polygon = polygons.get(i);
            info.append("Полигон ").append(i + 1).append(":\n");
            info.append("  ▸ Вершин: ").append(polygon.getVertexCount()).append("\n");
            info.append("  ▸ Тип: ").append(polygon.isConvex() ? "ВЫПУКЛЫЙ" : "НЕВЫПУКЛЫЙ").append("\n");

            if (cursorPoint != null && polygon.getVertexCount() >= 3) {
                boolean contains = polygon.containsPoint(cursorPoint);
                info.append("  ▸ Точка внутри: ").append(contains ? "ДА" : "НЕТ").append("\n");

                // Динамическая классификация относительно всех рёбер
                List<String> classifications = polygon.getPointEdgeClassifications(cursorPoint);
                info.append("  ▸ Положение относительно рёбер:\n");
                for (String classification : classifications) {
                    info.append("      ").append(classification).append("\n");
                }

                // Специальная проверка для выбранного ребра
                if (i == polygons.size() - 1 && selectedEdgeIndex != -1) {
                    info.append("  ▸ Проверка выбранного ребра ").append(selectedEdgeIndex + 1).append(":\n");
                    info.append("      ").append(polygon.checkPointAgainstEdge(cursorPoint, selectedEdgeIndex)).append("\n");
                }
            }
            info.append("\n");
        }

        if (selectedEdgeIndex != -1 && !polygons.isEmpty()) {
            info.append("ВЫБРАНО РЕБРО: ").append(selectedEdgeIndex + 1)
                    .append(" (между вершинами ").append(selectedEdgeIndex + 1)
                    .append(" и ").append((selectedEdgeIndex + 1) % currentPolygon.getVertexCount() + 1).append(")\n");
        }

        info.append("ИНСТРУКЦИЯ:\n");
        info.append("• Левая кнопка - добавить вершину полигона\n");
        info.append("• Движение мыши - динамическая проверка точки\n");
        info.append("• Кнопка 'Выбрать ребро' - выбрать ребро для детальной проверки\n");

        infoArea.setText(info.toString());
    }

    private void clearScene() {
        polygons.clear();
        currentPolygon.clear();
        cursorPoint = null;
        selectedEdgeIndex = -1;
        updateInfo();
        drawingPanel.repaint();
    }

    private void startNewPolygon() {
        currentPolygon = new Polygon();
        selectedEdgeIndex = -1;
        updateInfo();
        drawingPanel.repaint();
    }
}
