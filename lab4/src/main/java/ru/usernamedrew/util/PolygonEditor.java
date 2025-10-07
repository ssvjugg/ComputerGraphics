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
    private static class AffineMatrix {
        public static double[][] getTranslationMatrix(double dx, double dy) {
            return new double[][]{
                    {1, 0, dx},
                    {0, 1, dy},
                    {0, 0, 1}
            };
        }

        public static double[][] getRotationMatrix(double angleInRadians) {
            double cos = Math.cos(angleInRadians);
            double sin = Math.sin(angleInRadians);
            return new double[][]{
                    {cos, -sin, 0},
                    {sin, cos, 0},
                    {0, 0, 1}
            };
        }

        public static double[][] getScalingMatrix(double sx, double sy) {
            return new double[][]{
                    {sx, 0, 0},
                    {0, sy, 0},
                    {0, 0, 1}
            };
        }

        public static double[][] multiply(double[][] A, double[][] B) {
            double[][] C = new double[3][3];
            for (int i = 0; i < 3; i++) {
                for (int j = 0; j < 3; j++) {
                    C[i][j] = A[i][0] * B[0][j] + A[i][1] * B[1][j] + A[i][2] * B[2][j];
                }
            }
            return C;
        }
    }


    private final List<Polygon> polygons;
    private Polygon currentPolygon;
    private JPanel drawingPanel;
    private JTextArea infoArea; // Добавление текстовой области для информации
    private Point2D.Double cursorPoint; // Добавление отслеживания позиции курсора
    private int selectedEdgeIndex;// Добавление индекса выбранного ребра

    private boolean intersectionModeActive;
    private Point2D.Double dynamicEdgeStartPoint;
    private List<Point2D.Double> intersectionPoint;
    private JButton toggleIntersectionButton;


    public PolygonEditor() {
        polygons = new ArrayList<>();
        currentPolygon = new Polygon();

        // Начальные значения "неактивного состояния"
        cursorPoint = null;
        selectedEdgeIndex = -1;

        intersectionModeActive = false;
        dynamicEdgeStartPoint = null;
        intersectionPoint = null;

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

                drawIntersection(g2d);

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

                intersectionPoint = new ArrayList<>();

                if (intersectionModeActive && dynamicEdgeStartPoint != null &&
                        !currentPolygon.isEmpty() && currentPolygon.getVertexCount() >= 2) {
                    Point2D.Double p3 = dynamicEdgeStartPoint;
                    Point2D.Double p4 = cursorPoint;

                    List<Point2D.Double> vertices = currentPolygon.getVertices();
                    for (int i = 0; i < vertices.size(); i++) {
                        Point2D.Double a = vertices.get(i);
                        Point2D.Double b = vertices.get((i + 1) % vertices.size());

                        Point2D.Double intersection = currentPolygon.findIntersection(a, b, p3, p4);

                        if (intersection != null) {
                            intersectionPoint.add(intersection);
                        }
                    }
                }
                updateInfo();
                drawingPanel.repaint();
            }
        });

        // Панель информации
        infoArea = new JTextArea(15, 50);
        infoArea.setEditable(false);
        infoArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        JScrollPane scrollPane = new JScrollPane(infoArea);

        JPanel transformPanel = new JPanel();

        JButton translateButton = new JButton("Смещение");
        JButton rotateAroundPointButton = new JButton("Поворот (точка)");
        JButton rotateAroundCenterButton = new JButton("Поворот (центр)");
        JButton scaleAroundPointButton = new JButton("Масштаб (точка)");
        JButton scaleAroundCenterButton = new JButton("Масштаб (центр)");

        translateButton.addActionListener(e -> translatePolygon());
        rotateAroundPointButton.addActionListener(e -> rotateAroundUserPoint());
        rotateAroundCenterButton.addActionListener(e -> rotateAroundCenter());
        scaleAroundPointButton.addActionListener(e -> scaleAroundUserPoint());
        scaleAroundCenterButton.addActionListener(e -> scaleAroundCenter());

        transformPanel.add(translateButton);
        transformPanel.add(rotateAroundPointButton);
        transformPanel.add(rotateAroundCenterButton);
        transformPanel.add(scaleAroundPointButton);
        transformPanel.add(scaleAroundCenterButton);

        // Панель управления
        JPanel controlPanel = new JPanel();
        JButton clearButton = new JButton("Очистить сцену");
        JButton newPolygonButton = new JButton("Новый полигон");
        JButton selectEdgeButton = new JButton("Выбрать ребро для проверки");

        toggleIntersectionButton = new JButton("Включить режим пересечения");
        toggleIntersectionButton.addActionListener(e -> toggleIntersectionMode());

        clearButton.addActionListener(e -> clearScene());
        newPolygonButton.addActionListener(e -> startNewPolygon());
        selectEdgeButton.addActionListener(e -> selectEdgeForTesting());

        controlPanel.add(newPolygonButton);
        controlPanel.add(clearButton);
        controlPanel.add(selectEdgeButton);
        controlPanel.add(toggleIntersectionButton);

        JPanel mainControlPanel = new JPanel(new BorderLayout());
        mainControlPanel.add(controlPanel, BorderLayout.NORTH);
        mainControlPanel.add(transformPanel, BorderLayout.SOUTH);

        add(mainControlPanel, BorderLayout.NORTH);
        add(drawingPanel, BorderLayout.CENTER);
        add(scrollPane, BorderLayout.SOUTH);

        pack();
        setLocationRelativeTo(null);
    }

    private void handleMouseClick(MouseEvent e) {
        Point2D.Double point = new Point2D.Double(e.getX(), e.getY());

        if (!intersectionModeActive) {
            currentPolygon.addVertex(point);

            // Если это первая точка нового полигона, добавляем его в список
            if (currentPolygon.getVertexCount() == 1) {
                polygons.add(currentPolygon);
            }
        } else {
            if (dynamicEdgeStartPoint == null) {
                dynamicEdgeStartPoint = point;
            } else {
                dynamicEdgeStartPoint = null;
                intersectionPoint = null;
            }
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
            g2d.fillOval((int) vertex.x - 3, (int) vertex.y - 3, 6, 6);
            g2d.setColor(Color.BLACK);
            g2d.drawString(String.valueOf(i + 1), (int) vertex.x + 5, (int) vertex.y - 5);
            g2d.setColor(polygon.getColor());
        }

        // Рисуем рёбра с номерами
        if (vertices.size() >= 2) {
            for (int i = 0; i < vertices.size(); i++) {
                Point2D.Double p1 = vertices.get(i);
                Point2D.Double p2 = vertices.get((i + 1) % vertices.size());

                // Рисуем линию ребра
                g2d.drawLine((int) p1.x, (int) p1.y, (int) p2.x, (int) p2.y);

                // Подписываем ребро
                Point2D.Double midPoint = new Point2D.Double(
                        (p1.x + p2.x) / 2,
                        (p1.y + p2.y) / 2
                );
                g2d.setColor(Color.BLUE);
                g2d.drawString("Ребро " + (i + 1), (int) midPoint.x + 5, (int) midPoint.y - 5);
                g2d.setColor(polygon.getColor());

                // Выделяем выбранное ребро
                if (selectedEdgeIndex == i && polygonIndex == polygons.size() - 1) {
                    g2d.setStroke(new BasicStroke(3));
                    g2d.setColor(Color.RED);
                    g2d.drawLine((int) p1.x, (int) p1.y, (int) p2.x, (int) p2.y);
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
                    (int) center.x, (int) center.y);
        }
    }

    // Рисует крестик вокруг текущей позиции курсора
    //Помогает точно позиционировать точку для проверки
    private void drawCursorInfo(Graphics2D g2d) {
        g2d.setColor(Color.GRAY);
        g2d.drawLine((int) cursorPoint.x - 10, (int) cursorPoint.y,
                (int) cursorPoint.x + 10, (int) cursorPoint.y);
        g2d.drawLine((int) cursorPoint.x, (int) cursorPoint.y - 10,
                (int) cursorPoint.x, (int) cursorPoint.y + 10);

        // Подписываем координаты
        g2d.setColor(Color.BLACK);
        g2d.drawString(String.format("(%.1f, %.1f)", cursorPoint.x, cursorPoint.y),
                (int) cursorPoint.x + 15, (int) cursorPoint.y - 10);
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

        info.append("--- РЕЖИМ ПЕРЕСЕЧЕНИЯ ---\n");
        if (intersectionModeActive) {
            if (dynamicEdgeStartPoint != null) {
                info.append("  Начало ребра: (").append(String.format("%.1f", dynamicEdgeStartPoint.x))
                        .append(", ").append(String.format("%.1f", dynamicEdgeStartPoint.y)).append(")\n");

                // Проверяем, что список не пуст
                if (intersectionPoint != null && !intersectionPoint.isEmpty()) {
                    info.append("  РЕЗУЛЬТАТ: Найдено ").append(intersectionPoint.size()).append(" пересечений!\n");
                    // Выводим все найденные точки
                    for (int j = 0; j < intersectionPoint.size(); j++) {
                        Point2D.Double point = intersectionPoint.get(j);
                        info.append("    ▸ Точка ").append(j + 1).append(": (").append(String.format("%.1f", point.x))
                                .append(", ").append(String.format("%.1f", point.y)).append(")\n");
                    }
                } else {
                    info.append("  РЕЗУЛЬТАТ: Пересечение не найдено.\n");
                }
            } else {
                info.append("  Ожидание первого клика для установки начала динамического ребра.\n");
            }
        } else {
            info.append("СТАТУС: Отключен (Режим создания полигона).\n");
        }
        info.append("\n");

        info.append("ИНСТРУКЦИЯ:\n");
        info.append("• Левая кнопка - добавить вершину полигона\n");
        info.append("• Движение мыши - динамическая проверка точки\n");
        info.append("• Кнопка 'Выбрать ребро' - выбрать ребро для детальной проверки\n");

        infoArea.setText(info.toString());
    }


    private boolean checkPolygonExists() {
        if (polygons.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Создайте полигон, прежде чем применять преобразование.",
                    "Внимание", JOptionPane.WARNING_MESSAGE);
            return false;
        }
        return true;
    }

    private void translatePolygon() {
        if (!checkPolygonExists()) return;

        try {
            String dxStr = JOptionPane.showInputDialog(this, "Введите смещение по X (dx):");
            if (dxStr == null) return;
            double dx = Double.parseDouble(dxStr);

            String dyStr = JOptionPane.showInputDialog(this, "Введите смещение по Y (dy):");
            if (dyStr == null) return;
            double dy = Double.parseDouble(dyStr);

            double[][] matrix = AffineMatrix.getTranslationMatrix(dx, dy);
            currentPolygon.transform(matrix);

            updateInfo();
            drawingPanel.repaint();
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Некорректный ввод числа", "Ошибка", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void rotateAroundCenter() {
        if (!checkPolygonExists()) return;
        Point2D.Double center = currentPolygon.getCenter();
        rotatePolygon(center);
    }

    private void rotateAroundUserPoint() {
        if (!checkPolygonExists()) return;

        try {
            String xStr = JOptionPane.showInputDialog(this, "Введите X-координату центра поворота:");
            if (xStr == null) return;
            double px = Double.parseDouble(xStr);

            String yStr = JOptionPane.showInputDialog(this, "Введите Y-координату центра поворота:");
            if (yStr == null) return;
            double py = Double.parseDouble(yStr);

            rotatePolygon(new Point2D.Double(px, py));
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Некорректный ввод числа", "Ошибка", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void rotatePolygon(Point2D.Double rotationCenter) {
        try {
            String angleStr = JOptionPane.showInputDialog(this, "Введите угол поворота в градусах:");
            if (angleStr == null) return;
            double angleDegrees = Double.parseDouble(angleStr);
            double angleRadians = Math.toRadians(angleDegrees);

            double tx = rotationCenter.x;
            double ty = rotationCenter.y;


            double[][] T_neg = AffineMatrix.getTranslationMatrix(-tx, -ty);

            double[][] R = AffineMatrix.getRotationMatrix(angleRadians);

            double[][] T_pos = AffineMatrix.getTranslationMatrix(tx, ty);

            double[][] M = AffineMatrix.multiply(T_pos, AffineMatrix.multiply(R, T_neg));

            currentPolygon.transform(M);

            updateInfo();
            drawingPanel.repaint();
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Некорректный ввод числа", "Ошибка", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void scaleAroundCenter() {
        if (!checkPolygonExists()) return;
        Point2D.Double center = currentPolygon.getCenter();
        scalePolygon(center);
    }

    private void scaleAroundUserPoint() {
        if (!checkPolygonExists()) return;

        try {
            String xStr = JOptionPane.showInputDialog(this, "Введите X-координату центра масштабирования:");
            if (xStr == null) return;
            double px = Double.parseDouble(xStr);

            String yStr = JOptionPane.showInputDialog(this, "Введите Y-координату центра масштабирования:");
            if (yStr == null) return;
            double py = Double.parseDouble(yStr);

            scalePolygon(new Point2D.Double(px, py));
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Некорректный ввод числа", "Ошибка", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void scalePolygon(Point2D.Double scaleCenter) {
        try {
            String sxStr = JOptionPane.showInputDialog(this, "Введите коэффициент масштабирования по X (sx):");
            if (sxStr == null) return;
            double sx = Double.parseDouble(sxStr);

            String syStr = JOptionPane.showInputDialog(this, "Введите коэффициент масштабирования по Y (sy):");
            if (syStr == null) return;
            double sy = Double.parseDouble(syStr);

            double tx = scaleCenter.x;
            double ty = scaleCenter.y;

            double[][] T_neg = AffineMatrix.getTranslationMatrix(-tx, -ty);

            double[][] S = AffineMatrix.getScalingMatrix(sx, sy);

            double[][] T_pos = AffineMatrix.getTranslationMatrix(tx, ty);

            double[][] M = AffineMatrix.multiply(T_pos, AffineMatrix.multiply(S, T_neg));

            currentPolygon.transform(M);

            updateInfo();
            drawingPanel.repaint();
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Некорректный ввод числа", "Ошибка", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void drawIntersection(Graphics2D g2d) {
        if (intersectionModeActive && dynamicEdgeStartPoint != null && cursorPoint != null) {
            g2d.setColor(Color.MAGENTA);
            g2d.setStroke(new BasicStroke(2, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10.0f, new float[]{10.0f}, 0.0f));
            g2d.drawLine((int) dynamicEdgeStartPoint.x, (int) dynamicEdgeStartPoint.y,
                    (int) cursorPoint.x, (int) cursorPoint.y);
            g2d.setStroke(new BasicStroke(1));

            g2d.setColor(Color.MAGENTA);
            g2d.fillOval((int) dynamicEdgeStartPoint.x - 4, (int) dynamicEdgeStartPoint.y - 4, 8, 8);
            g2d.drawString("Начало 2 ребра", (int) dynamicEdgeStartPoint.x + 10, (int) dynamicEdgeStartPoint.y - 10);

            if (intersectionPoint != null && !intersectionPoint.isEmpty()) {
                for (Point2D.Double point : intersectionPoint) {
                    g2d.setColor(Color.RED);
                    g2d.fillOval((int) point.x - 5, (int) point.y - 5, 10, 10);
                    g2d.drawString("Пересечение", (int) point.x + 10, (int) point.y + 20);
                }
            }
        }
    }

    private void toggleIntersectionMode() {
        if (!intersectionModeActive) {
            if (currentPolygon.getVertexCount() < 2) {
                JOptionPane.showMessageDialog(this,
                        "Создайте полигон с хотя бы 2 вершинами, прежде чем включать режим пересечения.",
                        "Внимание", JOptionPane.WARNING_MESSAGE);
                return;
            }
            intersectionModeActive = true;
            toggleIntersectionButton.setText("Отключить режим пересечения");
        } else {
            intersectionModeActive = false;
            dynamicEdgeStartPoint = null;
            intersectionPoint = null;
            toggleIntersectionButton.setText("Включить режим пересечения");
        }
        updateInfo();
        drawingPanel.repaint();
    }

    private void clearScene() {
        polygons.clear();
        currentPolygon.clear();
        cursorPoint = null;
        selectedEdgeIndex = -1;

        intersectionModeActive = false;
        dynamicEdgeStartPoint = null;
        intersectionPoint = null;
        toggleIntersectionButton.setText("Включить режим пересечения");

        updateInfo();
        drawingPanel.repaint();
    }

    private void startNewPolygon() {
        currentPolygon = new Polygon();
        selectedEdgeIndex = -1;

        intersectionModeActive = false;
        dynamicEdgeStartPoint = null;
        intersectionPoint = null;
        toggleIntersectionButton.setText("Включить режим пересечения");

        updateInfo();
        drawingPanel.repaint();
    }
}
