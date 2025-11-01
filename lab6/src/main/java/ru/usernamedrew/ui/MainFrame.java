package ru.usernamedrew.ui;

import ru.usernamedrew.model.*;
import ru.usernamedrew.util.AffineTransform;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;

public class MainFrame extends JFrame {
    private GraphicsPanel graphicsPanel;
    private Polyhedron currentPolyhedron;

    public MainFrame() {
        initializeUI();
    }

    private void initializeUI() {
        setTitle("Лабораторная работа по 3D графике");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // Создаем панель для отрисовки
        graphicsPanel = new GraphicsPanel();
        add(graphicsPanel, BorderLayout.CENTER);

        // Создаем панель управления
        JPanel controlPanel = createControlPanel();
        add(controlPanel, BorderLayout.NORTH);

        pack();
        setLocationRelativeTo(null);
    }

    private JPanel createControlPanel() {
        JPanel panel = new JPanel(new FlowLayout());

        // Выбор многогранника
        JComboBox<String> polyhedronCombo = new JComboBox<>(new String[]{
                "Тетраэдр", "Гексаэдр", "Октаэдр"
        });

        polyhedronCombo.addActionListener(e -> {
            String selected = (String) polyhedronCombo.getSelectedItem();
            switch (selected) {
                case "Тетраэдр" -> currentPolyhedron = RegularPolyhedra.createTetrahedron();
                case "Гексаэдр" -> currentPolyhedron = RegularPolyhedra.createHexahedron();
                case "Октаэдр" -> currentPolyhedron = RegularPolyhedra.createOctahedron();
            }

            graphicsPanel.setPolyhedron(currentPolyhedron);
        });

        // Выбор проекции
        JComboBox<String> projectionCombo = new JComboBox<>(new String[]{
                "Аксонометрическая", "Перспективная"
        });

        projectionCombo.addActionListener(e -> {
            String selected = (String) projectionCombo.getSelectedItem();
            if (selected != null) {
                String projectionType = selected.equals("Аксонометрическая") ? "axonometric" : "perspective";
                graphicsPanel.setProjectionType(projectionType);
            }
        });

        // Кнопки преобразований
        JButton translateBtn = new JButton("Смещение");
        translateBtn.addActionListener(this::handleTranslation);

        JButton scaleBtn = new JButton("Масштабирование(центр)");
        scaleBtn.addActionListener(this::handleScalingCenter);

        JButton scaleOriginBtn = new JButton("Масштабирование(начало)");
        scaleOriginBtn.addActionListener(this::handleScalingOrigin);

        JButton rotateBtn = new JButton("Вращение");
        rotateBtn.addActionListener(this::handleRotation);

        JButton ownAxisRotateBtn = new JButton("Вращение вокруг своей оси");
        ownAxisRotateBtn.addActionListener(this::handleRotationAroundOwnAxis);

        JButton reflectBtn = new JButton("Отражение");
        reflectBtn.addActionListener(this::handleReflection);

        JButton arbitraryRotateBtn = new JButton("Вращение по произвольной оси");
        arbitraryRotateBtn.addActionListener(this::handleArbitraryRotation);

        // Добавляем компоненты
        panel.add(new JLabel("Многогранник:"));
        panel.add(polyhedronCombo);
        panel.add(new JLabel("Проекция:"));
        panel.add(projectionCombo);
        panel.add(translateBtn);
        panel.add(scaleBtn);
        panel.add(scaleOriginBtn);
        panel.add(rotateBtn);
        panel.add(ownAxisRotateBtn);
        panel.add(reflectBtn);

        panel.add(arbitraryRotateBtn);

        return panel;
    }

    private void handleTranslation(ActionEvent e) {
        if (currentPolyhedron == null) return;

        String xStr = JOptionPane.showInputDialog("Введите смещение по X:");
        String yStr = JOptionPane.showInputDialog("Введите смещение по Y:");
        String zStr = JOptionPane.showInputDialog("Введите смещение по Z:");

        try {
            if (xStr == null || yStr == null || zStr == null) {
                return;
            }
            double dx = Double.parseDouble(xStr);
            double dy = Double.parseDouble(yStr);
            double dz = Double.parseDouble(zStr);

            double[][] matrix = AffineTransform.createTranslationMatrix(dx, dy, dz);
            currentPolyhedron = currentPolyhedron.transform(matrix);
            graphicsPanel.setPolyhedron(currentPolyhedron);
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Неверный ввод!");
        }
    }

    private void scaleAroundPoint(Point3D point, double scale) {
        if (currentPolyhedron == null) return;

        try {
            double[][] T_neg = AffineTransform.createTranslationMatrix(-point.x(), -point.y(), -point.z());
            double[][] S = AffineTransform.createScalingMatrix(scale, scale, scale);
            double[][] T_pos = AffineTransform.createTranslationMatrix(point.x(), point.y(), point.z());

            double[][] M1 = AffineTransform.multiplyMatrices(S, T_neg);
            double[][] transform = AffineTransform.multiplyMatrices(T_pos, M1);

            currentPolyhedron = currentPolyhedron.transform(transform);
            graphicsPanel.setPolyhedron(currentPolyhedron);
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Неверный ввод!");
        }
    }

    private void handleScalingOrigin(ActionEvent e) {
        if (currentPolyhedron == null) return;

        String scaleStr = JOptionPane.showInputDialog("Введите коэффициент масштабирования:");

        try {
            if (scaleStr == null || scaleStr.isEmpty()) {
                return;
            }
            double scale = Double.parseDouble(scaleStr);
            Point3D origin = new Point3D(0, 0, 0);

            scaleAroundPoint(origin, scale);
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Неверный ввод!");
        }
    }

    private void handleScalingCenter(ActionEvent e) {
        if (currentPolyhedron == null) return;

        String scaleStr = JOptionPane.showInputDialog("Введите коэффициент масштабирования:");

        try {
            if (scaleStr == null || scaleStr.isEmpty()) {
                return;
            }
            double scale = Double.parseDouble(scaleStr);

            scaleAroundPoint(AffineTransform.getCenter(currentPolyhedron.getVertices()), scale);
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Неверный ввод!");
        }
    }

    private void rotateAroundPoint(double[][] rotationMatrix, Point3D point) {
        try {

            double[][] T_neg = AffineTransform.createTranslationMatrix(-point.x(), -point.y(), -point.z());
            double[][] T_pos = AffineTransform.createTranslationMatrix(point.x(), point.y(), point.z());

            double[][] M1 = AffineTransform.multiplyMatrices(rotationMatrix, T_neg);
            double[][] transform = AffineTransform.multiplyMatrices(T_pos, M1);

            currentPolyhedron = currentPolyhedron.transform(transform);
            graphicsPanel.setPolyhedron(currentPolyhedron);
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Неверный ввод!");
        }
    }

    private void handleRotation(ActionEvent e) {
        if (currentPolyhedron == null) return;

        String[] options = {"Ось X", "Ось Y", "Ось Z"};
        String axis = (String) JOptionPane.showInputDialog(this, "Выберите ось вращения:",
                "Вращение", JOptionPane.QUESTION_MESSAGE, null, options, options[0]);

        String angleStr = JOptionPane.showInputDialog("Введите угол в градусах:");

        try {
            if (angleStr == null || angleStr.isEmpty()) {
                return;
            }
            double angle = Math.toRadians(Double.parseDouble(angleStr));
            double[][] rotationMatrix;

            switch (axis) {
                case "Ось X" -> rotationMatrix = AffineTransform.createRotationXMatrix(angle);
                case "Ось Y" -> rotationMatrix = AffineTransform.createRotationYMatrix(angle);
                case "Ось Z" -> rotationMatrix = AffineTransform.createRotationZMatrix(angle);
                default -> rotationMatrix = AffineTransform.createIdentityMatrix();
            }
            Point3D point = new Point3D(0, 0, 0);

            rotateAroundPoint(rotationMatrix, point);
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Неверный ввод!");
        }
    }

    private void handleRotationAroundOwnAxis(ActionEvent e) {
        if (currentPolyhedron == null) return;

        String[] options = {"Ось X", "Ось Y", "Ось Z"};
        String axis = (String) JOptionPane.showInputDialog(this, "Выберите ось вращения:",
                "Вращение", JOptionPane.QUESTION_MESSAGE, null, options, options[0]);

        String angleStr = JOptionPane.showInputDialog("Введите угол в градусах:");

        try {
            if (angleStr == null || angleStr.isEmpty()) {
                return;
            }
            double angle = Math.toRadians(Double.parseDouble(angleStr));
            double[][] rotationMatrix;

            switch (axis) {
                case "Ось X" -> rotationMatrix = AffineTransform.createRotationXMatrix(angle);
                case "Ось Y" -> rotationMatrix = AffineTransform.createRotationYMatrix(angle);
                case "Ось Z" -> rotationMatrix = AffineTransform.createRotationZMatrix(angle);
                default -> rotationMatrix = AffineTransform.createIdentityMatrix();
            }
            Point3D center = AffineTransform.getCenter(currentPolyhedron.getVertices());

            rotateAroundPoint(rotationMatrix, center);
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Неверный ввод!");
        }
    }

    private void handleReflection(ActionEvent e) {
        if (currentPolyhedron == null) return;

        String[] options = {"Плоскость XY", "Плоскость XZ", "Плоскость YZ"};
        String plane = (String) JOptionPane.showInputDialog(this, "Выберите плоскость отражения:",
                "Отражение", JOptionPane.QUESTION_MESSAGE, null, options, options[0]);

        String planeCode = plane.split(" ")[1].toLowerCase();
        double[][] matrix = AffineTransform.createReflectionMatrix(planeCode);
        currentPolyhedron = currentPolyhedron.transform(matrix);
        graphicsPanel.setPolyhedron(currentPolyhedron);
    }

    private void handleArbitraryRotation(ActionEvent e) {
        if (currentPolyhedron == null) return;

        String axStr = JOptionPane.showInputDialog(this, "Введите координату A.x:");
        String ayStr = JOptionPane.showInputDialog(this, "Введите координату A.y:");
        String azStr = JOptionPane.showInputDialog(this, "Введите координату A.z:");

        String vxStr = JOptionPane.showInputDialog(this, "Введите компоненту V.l (X):");
        String vyStr = JOptionPane.showInputDialog(this, "Введите компоненту V.m (Y):");
        String vzStr = JOptionPane.showInputDialog(this, "Введите компоненту V.n (Z):");

        String angleStr = JOptionPane.showInputDialog(this, "Введите угол в градусах:");

        try {
            if (axStr == null || ayStr == null || azStr == null || vxStr == null || vyStr == null) {
                return;
            }
            double ax = Double.parseDouble(axStr);
            double ay = Double.parseDouble(ayStr);
            double az = Double.parseDouble(azStr);
            Point3D A = new Point3D(ax, ay, az);

            double vx = Double.parseDouble(vxStr);
            double vy = Double.parseDouble(vyStr);
            double vz = Double.parseDouble(vzStr);
            double angle = Math.toRadians(Double.parseDouble(angleStr));

            Point3D V = normalizeVector(new Point3D(vx, vy, vz));

            double[][] matrix = AffineTransform.createRotationAroundArbitraryAxis(A, V, angle);

            currentPolyhedron = currentPolyhedron.transform(matrix);
            graphicsPanel.setPolyhedron(currentPolyhedron);

        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Ошибка.");
        }
    }

    private Point3D normalizeVector(Point3D V) {
        double length = Math.sqrt(V.x() * V.x() + V.y() * V.y() + V.z() * V.z());

        if (length < 1e-9) {
            return new Point3D(0, 0, 1);
        }

        return new Point3D(V.x() / length, V.y() / length, V.z() / length);
    }
}