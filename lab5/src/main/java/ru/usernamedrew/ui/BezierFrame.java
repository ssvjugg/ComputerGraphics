package ru.usernamedrew.ui;

import ru.usernamedrew.bezier.BezierSpline;

import javax.swing.*;
import java.awt.*;

public class BezierFrame extends JFrame {
    private BezierSpline bezierPanel;

    public BezierFrame() {
        setTitle("Кубические сплайны Безье");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(800, 600);
        setLocationRelativeTo(null);

        initializeComponents();
    }

    private void initializeComponents() {
        bezierPanel = new BezierSpline();

        // Панель управления
        JPanel controlPanel = new JPanel();

        JButton clearButton = new JButton("Очистить все точки");
        clearButton.addActionListener(e -> bezierPanel.clearPoints());

        JCheckBox linesCheckbox = new JCheckBox("Показать контрольные линии", true);
        linesCheckbox.addActionListener(e -> bezierPanel.setShowControlLines(linesCheckbox.isSelected()));

        JCheckBox pointsCheckbox = new JCheckBox("Показать контрольные точки", true);
        pointsCheckbox.addActionListener(e -> bezierPanel.setShowControlPoints(pointsCheckbox.isSelected()));

        JLabel infoLabel = new JLabel("Управление: ЛКМ - добавить/переместить | Ctrl+ЛКМ - удалить");
        infoLabel.setForeground(Color.DARK_GRAY);

        controlPanel.add(clearButton);
        controlPanel.add(linesCheckbox);
        controlPanel.add(pointsCheckbox);
        controlPanel.add(infoLabel);

        setLayout(new BorderLayout());
        add(bezierPanel, BorderLayout.CENTER);
        add(controlPanel, BorderLayout.SOUTH);
    }


}