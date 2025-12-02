package ru.usernamedrew.ui;

import ru.usernamedrew.controller.CameraController;
import ru.usernamedrew.model.*;
import ru.usernamedrew.util.AffineTransform;
import ru.usernamedrew.util.PolyhedronIO;
import ru.usernamedrew.util.ZBuffer;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseListener;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.List; // Добавьте этот импорт
import java.util.ArrayList; // И этот

public class MainFrame extends JFrame {
    private GraphicsPanel graphicsPanel;
    private Polyhedron currentPolyhedron;
    private Camera camera;
    private CameraController cameraController;
    private boolean cameraMode = false;

    public MainFrame() {
        initializeUI();
    }

    private void initializeUI() {
        setTitle("Лабораторная работа по 3D графике");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // Создаем панель для отрисовки
        graphicsPanel = new GraphicsPanel();

        // Создаем основную панель управления с прокруткой
        JPanel mainControlPanel = createMainControlPanel();
        JScrollPane scrollPane = new JScrollPane(mainControlPanel);
        scrollPane.setPreferredSize(new Dimension(1000, 150)); // Увеличиваем высоту
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER);

        // Добавляем компоненты
        add(scrollPane, BorderLayout.NORTH);
        add(graphicsPanel, BorderLayout.CENTER);

        pack();
        setLocationRelativeTo(null);
        setExtendedState(JFrame.MAXIMIZED_BOTH); // Разворачиваем на весь экран
    }

    private JPanel createMainControlPanel() {
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));

        // Создаем панель с вкладками
        JTabbedPane tabbedPane = new JTabbedPane();

        // Вкладка 1: Основные объекты
        JPanel objectsTab = new JPanel();
        objectsTab.setLayout(new BoxLayout(objectsTab, BoxLayout.Y_AXIS));

        JPanel revolutionPanel = createRevolutionControlPanel();
        JPanel basicControlPanel = createBasicControlPanel();
        JPanel zBufferPanel = createZBufferControlPanel(); // ДОБАВЬТЕ ЭТУ СТРОКУ

        objectsTab.add(revolutionPanel);
        objectsTab.add(basicControlPanel);
        objectsTab.add(zBufferPanel); // ДОБАВЬТЕ ЭТУ СТРОКУ

        // Вкладка 2: Преобразования и камера
        JPanel transformTab = new JPanel();
        transformTab.setLayout(new BoxLayout(transformTab, BoxLayout.Y_AXIS));

        JPanel transformPanel = createTransformControlPanel();
        JPanel cameraPanel = createCameraControlPanel();

        transformTab.add(transformPanel);
        transformTab.add(cameraPanel);

        // Вкладка 3: Освещение
        JPanel lightingTab = new JPanel();
        lightingTab.setLayout(new BoxLayout(lightingTab, BoxLayout.Y_AXIS));
        JPanel lightingPanel = createLightingControlPanel();
        lightingTab.add(lightingPanel);

        tabbedPane.addTab("Объекты", objectsTab);
        tabbedPane.addTab("Преобразования", transformTab);
        tabbedPane.addTab("Освещение", lightingTab);

        mainPanel.add(tabbedPane);
        return mainPanel;
    }

    private JPanel createTransformControlPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        panel.setPreferredSize(new Dimension(800, 40));

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

        panel.add(translateBtn);
        panel.add(scaleBtn);
        panel.add(scaleOriginBtn);
        panel.add(rotateBtn);
        panel.add(ownAxisRotateBtn);
        panel.add(reflectBtn);
        panel.add(arbitraryRotateBtn);

        return panel;
    }

    private JPanel createCameraControlPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        panel.setPreferredSize(new Dimension(800, 40));

        JButton cameraModeBtn = new JButton("Режим камеры");
        cameraModeBtn.addActionListener(e -> toggleCameraMode());

        // Чекбокс для отсечения граней
        JCheckBox backfaceCullingCheckbox = new JCheckBox("Отсечение нелицевых граней", true);
        backfaceCullingCheckbox.addActionListener(e -> {
            graphicsPanel.setBackfaceCulling(backfaceCullingCheckbox.isSelected());
            graphicsPanel.requestFocusInWindow();
        });

        panel.add(cameraModeBtn);
        panel.add(backfaceCullingCheckbox);

        return panel;
    }

    // Создаем панель управления освещением
    private JPanel createLightingControlPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        panel.setPreferredSize(new Dimension(1000, 40));

        JButton lightingBtn = new JButton("Управление освещением");
        lightingBtn.addActionListener(e -> showLightingDialog());
        panel.add(lightingBtn);

        JButton colorBtn = new JButton("Цвет объекта");
        colorBtn.addActionListener(e -> chooseObjectColor());
        panel.add(colorBtn);

        JComboBox<String> shadingCombo = new JComboBox<>(new String[]{
                "Фонг + Блинн-Фонг",
                "Гуро + Ламберт",
                "Фонг + Тун-шейдинг"
        });

        shadingCombo.addActionListener(e -> {
            String selected = (String) shadingCombo.getSelectedItem();
            ZBuffer.ShadingMode mode = switch (selected) {
                case "Гуро + Ламберт" -> ZBuffer.ShadingMode.GOURAUD_LAMBERT;
                case "Фонг + Тун-шейдинг" -> ZBuffer.ShadingMode.PHONG_TOON;
                default -> ZBuffer.ShadingMode.DEFAULT;
            };
            // Нужно убедиться, что Z-буфер включен, иначе эффекта не будет видно
            if (!graphicsPanel.isZBufferEnabled()) {
                JOptionPane.showMessageDialog(this, "Включите Z-буфер для просмотра шейдинга!");
            }
            graphicsPanel.setShadingMode(mode);
        });

        panel.add(new JLabel("Режим:"));
        panel.add(shadingCombo);

        return panel;
    }

    private void showLightingDialog() {
        JDialog dialog = new JDialog(this, "Управление освещением", true);
        dialog.setLayout(new GridLayout(0, 2, 5, 5));
        dialog.setSize(400, 200);

        JTextField dirX = new JTextField("1");
        JTextField dirY = new JTextField("1");
        JTextField dirZ = new JTextField("-1");
        JTextField dirIntensity = new JTextField("0.7");
        JTextField ambIntensity = new JTextField("0.3");

        dialog.add(new JLabel("Направленный свет (X,Y,Z):"));
        JPanel dirPanel = new JPanel(new FlowLayout());
        dirPanel.add(dirX); dirPanel.add(dirY); dirPanel.add(dirZ);
        dialog.add(dirPanel);

        dialog.add(new JLabel("Интенсивность направленного:"));
        dialog.add(dirIntensity);

        dialog.add(new JLabel("Интенсивность окружающего:"));
        dialog.add(ambIntensity);

        JButton applyBtn = new JButton("Применить");
        applyBtn.addActionListener(e -> {
            try {
                Point3D lightDir = new Point3D(
                        Double.parseDouble(dirX.getText()),
                        Double.parseDouble(dirY.getText()),
                        Double.parseDouble(dirZ.getText())
                ).normalize();

                // Используем полное имя java.util.List
                java.util.List<Light> lights = new java.util.ArrayList<>();
                lights.add(new Light(new Color(255, 255, 255),
                        Double.parseDouble(ambIntensity.getText())));
                lights.add(new Light(lightDir, new Color(255, 255, 255),
                        Double.parseDouble(dirIntensity.getText())));

                graphicsPanel.setLights(lights);
                dialog.dispose();
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(dialog, "Неверный формат числа!");
            }
        });
        graphicsPanel.requestFocusInWindow();
        dialog.add(applyBtn);
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

    private void chooseObjectColor() {
        if (currentPolyhedron == null) {
            JOptionPane.showMessageDialog(this, "Сначала выберите объект!");
            return;
        }

        Color newColor = JColorChooser.showDialog(this, "Выберите цвет объекта",
                currentPolyhedron.getColor());

        if (newColor != null) {
            currentPolyhedron.setColor(newColor);
            graphicsPanel.updateActivePolyhedron(currentPolyhedron);
        }
        graphicsPanel.requestFocusInWindow();
    }

    private JPanel createZBufferControlPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        panel.setPreferredSize(new Dimension(800, 40));

        // Кнопка включения/выключения z-буфера
        JButton zBufferToggleBtn = new JButton("Включить Z-буфер");
        zBufferToggleBtn.addActionListener(e -> toggleZBuffer(zBufferToggleBtn));
        panel.add(zBufferToggleBtn);

        return panel;
    }

    private void toggleZBuffer(JButton button) {
        boolean currentlyEnabled = graphicsPanel.isZBufferEnabled();
        graphicsPanel.setZBufferEnabled(!currentlyEnabled);

        if (!currentlyEnabled) {
            button.setText("Выключить Z-буфер");
        } else {
            button.setText("Включить Z-буфер");
        }
        graphicsPanel.requestFocusInWindow();
        graphicsPanel.repaint();
    }

    private JPanel createBasicControlPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        panel.setPreferredSize(new Dimension(800, 40));

        // Только самые основные элементы
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
            currentPolyhedron.recalculateNormals();
            graphicsPanel.addPolyhedron(currentPolyhedron);
            graphicsPanel.requestFocusInWindow();
        });

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

        panel.add(new JLabel("Фигура:"));
        panel.add(polyhedronCombo);
        panel.add(new JLabel("Проекция:"));
        panel.add(projectionCombo);

        return panel;
    }

    private void toggleCameraMode() {
        cameraMode = !cameraMode;

        if (cameraMode) {
            // Инициализация камеры и контроллера
            camera = new Camera(new Point3D(0, 0, 5), -90, 0);
            cameraController = new CameraController(camera, graphicsPanel);
            graphicsPanel.setProjectionType("perspective");
            graphicsPanel.setCamera(camera);

            graphicsPanel.addKeyListener(cameraController);
            graphicsPanel.setFocusable(true);
            graphicsPanel.requestFocusInWindow();
            JOptionPane.showMessageDialog(this, "Режим камеры включен. Используйте WASD, SPACE, SHIFT и мышь для управления.");
        } else {
            // Выключение режима камеры
            if (cameraController != null) {
                graphicsPanel.removeKeyListener(cameraController);
                graphicsPanel.removeMouseListener((MouseListener) cameraController);
                graphicsPanel.removeMouseMotionListener(cameraController);
                cameraController = null;
            }
            JOptionPane.showMessageDialog(this, "Режим камеры выключен.");
        }

        graphicsPanel.requestFocusInWindow();
    }

    private Point3D handleManualViewVectorInput() {
        String xStr = JOptionPane.showInputDialog("Введите X компонент вектора обзора:\n(например: 1, 0, -1)");
        String yStr = JOptionPane.showInputDialog("Введите Y компонент вектора обзора:");
        String zStr = JOptionPane.showInputDialog("Введите Z компонент вектора обзора:");

        try {
            if (xStr == null || yStr == null || zStr == null) return null;

            double x = Double.parseDouble(xStr);
            double y = Double.parseDouble(yStr);
            double z = Double.parseDouble(zStr);

            // Нормализуем вектор
            double length = Math.sqrt(x*x + y*y + z*z);
            if (length < 1e-9) {
                JOptionPane.showMessageDialog(this, "Вектор не может быть нулевым! Используется (0,0,-1)");
                return new Point3D(0, 0, -1);
            }

            return new Point3D(x/length, y/length, z/length);
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Неверный формат числа! Используется стандартный вектор (0,0,-1)");
            return new Point3D(0, 0, -1);
        }
    }

    // Обработчик автоматического вращения
    private void handleAutoRotation(ActionEvent e) {
        if (currentPolyhedron == null) return;

        Thread rotationThread = new Thread(() -> {
            try {
                for (int i = 0; i < 360; i += 5) {
                    double angle = Math.toRadians(i);
                    double[][] rotationMatrix = AffineTransform.createRotationYMatrix(angle);
                    currentPolyhedron = currentPolyhedron.transform(rotationMatrix);

                    SwingUtilities.invokeLater(() -> {
                        graphicsPanel.setPolyhedron(currentPolyhedron);
                    });

                    Thread.sleep(50);
                }
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            }
        });

        rotationThread.setDaemon(true);
        rotationThread.start();
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
            currentPolyhedron.recalculateNormals();
            graphicsPanel.updateActivePolyhedron(currentPolyhedron);
            graphicsPanel.requestFocusInWindow();
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
            currentPolyhedron.recalculateNormals();
            graphicsPanel.updateActivePolyhedron(currentPolyhedron);
            graphicsPanel.requestFocusInWindow();
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
            currentPolyhedron.recalculateNormals();
            graphicsPanel.updateActivePolyhedron(currentPolyhedron);
            graphicsPanel.requestFocusInWindow();
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
        currentPolyhedron.recalculateNormals();
        graphicsPanel.updateActivePolyhedron(currentPolyhedron);
        graphicsPanel.requestFocusInWindow();
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
            currentPolyhedron.recalculateNormals();
            graphicsPanel.updateActivePolyhedron(currentPolyhedron);
            graphicsPanel.requestFocusInWindow();
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Ошибка.");
        }
    }

    private JPanel createSurfaceControlPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT));

        JComboBox<String> functionCombo = new JComboBox<>(new String[]{
                "Параболоид (x^2 + y^2)", "Синусоида (sin(x) * cos(y))"
        });

        JButton buildSurfaceBtn = new JButton("Построить Поверхность");
        buildSurfaceBtn.addActionListener(e -> handleSurfaceCreation(functionCombo));

        panel.add(new JLabel("Поверхность:"));
        panel.add(functionCombo);
        panel.add(buildSurfaceBtn);
        graphicsPanel.requestFocusInWindow();
        return panel;
    }

    private void handleSurfaceCreation(JComboBox<String> functionCombo) {
        String selectedFunction = (String) functionCombo.getSelectedItem();
        BiFunction<Double, Double, Double> function;

        if (selectedFunction == null) return;

        if (selectedFunction.contains("Параболоид")) {
            function = SurfaceFactory::paraboloid;
        } else if (selectedFunction.contains("Синусоида")) {
            function = SurfaceFactory::sinCosSurface;
        } else {
            JOptionPane.showMessageDialog(this, "Неизвестная функция.");
            return;
        }

        String x0Str = JOptionPane.showInputDialog("Введите X0:");
        String x1Str = JOptionPane.showInputDialog("Введите X1:");
        String y0Str = JOptionPane.showInputDialog("Введите Y0:");
        String y1Str = JOptionPane.showInputDialog("Введите Y1:");
        String nStr = JOptionPane.showInputDialog("Введите количество разбиений (Nx, Ny):");

        try {
            if (x0Str == null || x1Str == null || y0Str == null || y1Str == null || nStr == null) {
                return;
            }

            double x0 = Double.parseDouble(x0Str);
            double x1 = Double.parseDouble(x1Str);
            double y0 = Double.parseDouble(y0Str);
            double y1 = Double.parseDouble(y1Str);
            int n = Integer.parseInt(nStr);

            if (n <= 0) {
                JOptionPane.showMessageDialog(this, "Колво разбиений должно быть > 0");
                return;
            }

            currentPolyhedron = SurfaceFactory.createSurface(function, x0, x1, y0, y1, n, n);
            currentPolyhedron.recalculateNormals();
            graphicsPanel.addPolyhedron(currentPolyhedron);
            graphicsPanel.requestFocusInWindow();
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Ошибка");
        }
    }

    private Point3D normalizeVector(Point3D V) {
        double length = Math.sqrt(V.x() * V.x() + V.y() * V.y() + V.z() * V.z());

        if (length < 1e-9) {
            return new Point3D(0, 0, 1);
        }

        return new Point3D(V.x() / length, V.y() / length, V.z() / length);
    }

    private JPanel createRevolutionControlPanel() {
        // Создает UI элементы:
        // - Выбор типа фигуры (Цилиндр/Конус/Сфера/Пользовательская)
        // - Выбор оси вращения (X/Y/Z)
        // - Поле для ввода количества разбиений
        // - Кнопки: "Построить фигуру вращения", "Сохранить модель", "Загрузить модель"
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT));

        JComboBox<String> shapeCombo = new JComboBox<>(new String[]{
                "Цилиндр", "Конус", "Сфера", "Пользовательская"
        });

        JComboBox<String> axisCombo = new JComboBox<>(new String[]{
                "Ось X", "Ось Y", "Ось Z"
        });

        JTextField divisionsField = new JTextField("24", 5);

        JButton buildRevolutionBtn = new JButton("Построить фигуру вращения");
        buildRevolutionBtn.addActionListener(e -> handleRevolutionCreation(
                shapeCombo, axisCombo, divisionsField));

        JButton saveBtn = new JButton("Сохранить модель");
        saveBtn.addActionListener(e -> handleSaveModel());

        JButton loadBtn = new JButton("Загрузить модель");
        loadBtn.addActionListener(e -> handleLoadModel());

        panel.add(new JLabel("Фигура вращения:"));
        panel.add(shapeCombo);
        panel.add(new JLabel("Ось:"));
        panel.add(axisCombo);
        panel.add(new JLabel("Разбиения:"));
        panel.add(divisionsField);
        panel.add(buildRevolutionBtn);
        panel.add(saveBtn);
        panel.add(loadBtn);

        return panel;
    }

    private void handleRevolutionCreation(JComboBox<String> shapeCombo, JComboBox<String> axisCombo, JTextField divisionsField) {
        // 1. Получает выбранные параметры из UI
        // 2. Проверяет корректность ввода (разбиения ≥ 3)
        // 3. Определяет ось вращения (X/Y/Z)
        // 4. Создает образующую в зависимости от типа фигуры
        // 5. Вызывает RevolutionSurfaceFactory для построения фигуры
        // 6. Устанавливает полученную модель в graphicsPanel
            try {
            String shapeType = (String) shapeCombo.getSelectedItem();
            String axisStr = (String) axisCombo.getSelectedItem();
            int divisions = Integer.parseInt(divisionsField.getText());

            if (divisions < 3) {
                JOptionPane.showMessageDialog(this, "Количество разбиений должно быть не менее 3");
                return;
            }

            RevolutionSurfaceFactory.Axis axis = switch (axisStr) {
                case "Ось X" -> RevolutionSurfaceFactory.Axis.X;
                case "Ось Y" -> RevolutionSurfaceFactory.Axis.Y;
                case "Ось Z" -> RevolutionSurfaceFactory.Axis.Z;
                default -> RevolutionSurfaceFactory.Axis.Y;
            };

            java.util.List<Point3D> generatrix;

            switch (shapeType) {
                case "Цилиндр" -> generatrix = RevolutionSurfaceFactory.createCylinderGeneratrix(1.0, 2.0, 10);
                case "Конус" -> generatrix = RevolutionSurfaceFactory.createConeGeneratrix(1.0, 2.0, 10);
                case "Сфера" -> generatrix = RevolutionSurfaceFactory.createSphereGeneratrix(1.0, 10);
                case "Пользовательская" -> {
                    generatrix = createCustomGeneratrix();
                    if (generatrix == null) return;
                }
                default -> {
                    JOptionPane.showMessageDialog(this, "Неизвестный тип фигуры");
                    return;
                }
            }

            currentPolyhedron = RevolutionSurfaceFactory.createRevolutionSurface(generatrix, axis, divisions);
            currentPolyhedron.recalculateNormals();
            graphicsPanel.addPolyhedron(currentPolyhedron);
            graphicsPanel.requestFocusInWindow();
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Неверный формат числа");
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Ошибка при создании фигуры: " + ex.getMessage());
        }
    }

    private java.util.List<Point3D> createCustomGeneratrix() {
        // 1. Запрашивает у пользователя точки в формате: "x1,y1,z1;x2,y2,z2;..."
        // 2. Парсит строку в список Point3D
        // 3. Проверяет, что минимум 2 точки
        // 4. Возвращает список точек образующей
        String input = JOptionPane.showInputDialog(this,
                "Введите точки образующей в формате:\n" +
                        "x1,y1,z1;x2,y2,z2;...\n" +
                        "Пример: 0,0,0;1,0,0;1,1,0");

        if (input == null || input.trim().isEmpty()) {
            return null;
        }

        java.util.List<Point3D> generatrix = new java.util.ArrayList<>();
        String[] points = input.split(";");

        for (String pointStr : points) {
            String[] coords = pointStr.split(",");
            if (coords.length == 3) {
                try {
                    double x = Double.parseDouble(coords[0].trim());
                    double y = Double.parseDouble(coords[1].trim());
                    double z = Double.parseDouble(coords[2].trim());
                    generatrix.add(new Point3D(x, y, z));
                } catch (NumberFormatException e) {
                    JOptionPane.showMessageDialog(this, "Неверный формат координат: " + pointStr);
                    return null;
                }
            }
        }

        if (generatrix.size() < 2) {
            JOptionPane.showMessageDialog(this, "Образующая должна содержать хотя бы 2 точки");
            return null;
        }

        return generatrix;
    }

    private void handleSaveModel() {
        // 1. Проверяет, что есть модель для сохранения
        // 2. Открывает диалог выбора файла
        // 3. Вызывает PolyhedronIO.saveToFile()
        // 4. Показывает сообщение об успехе/ошибке
        if (currentPolyhedron == null) {
            JOptionPane.showMessageDialog(this, "Нет модели для сохранения");
            return;
        }

        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Сохранить модель");

        if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            try {
                PolyhedronIO.saveToFile(currentPolyhedron, fileChooser.getSelectedFile().getAbsolutePath());
                graphicsPanel.requestFocusInWindow();
                JOptionPane.showMessageDialog(this, "Модель успешно сохранена");
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this, "Ошибка при сохранении: " + ex.getMessage());
            }
        }
    }

    private void handleLoadModel() {
        // 1. Открывает диалог выбора файла
        // 2. Вызывает PolyhedronIO.loadFromFile()
        // 3. Устанавливает загруженную модель в graphicsPanel
        // 4. Показывает сообщение об успехе/ошибке
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Загрузить модель");

        if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            try {
                currentPolyhedron = PolyhedronIO.loadFromFile(fileChooser.getSelectedFile().getAbsolutePath());
                graphicsPanel.requestFocusInWindow();
                graphicsPanel.addPolyhedron(currentPolyhedron);
                JOptionPane.showMessageDialog(this, "Модель успешно загружена");
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this, "Ошибка при загрузке: " + ex.getMessage());
            }
        }
    }
}