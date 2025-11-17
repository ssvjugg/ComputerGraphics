package ru.usernamedrew.controller;

import ru.usernamedrew.model.Camera;

import javax.swing.*;
import java.awt.event.*;

public class CameraController implements KeyListener, MouseMotionListener {
    private final Camera camera;
    private final JComponent component;

    private boolean forward, backward, left, right, up, down;
    private double mouseSensitivity = 0.1;
    private int lastMouseX, lastMouseY;
    private boolean rotating = false;

    private final Timer updateTimer;

    public CameraController(Camera camera, JComponent component) {
        this.camera = camera;
        this.component = component;

        updateTimer = new Timer(16, e -> updateCamera());
        updateTimer.start();

        component.addMouseMotionListener(this);
        component.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                rotating = true;
                lastMouseX = e.getX();
                lastMouseY = e.getY();
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                rotating = false;
            }
        });
    }

    private void updateCamera() {
        double speed = 0.1;

        if (forward) camera.moveForward(speed);
        if (backward) camera.moveForward(-speed);
        if (right) camera.moveRight(speed);
        if (left) camera.moveRight(-speed);
        if (up) camera.moveUp(speed);
        if (down) camera.moveUp(-speed);

        component.repaint();
    }

    @Override
    public void keyPressed(KeyEvent e) {
        switch (e.getKeyCode()) {
            case KeyEvent.VK_W -> forward = true;
            case KeyEvent.VK_S -> backward = true;
            case KeyEvent.VK_A -> left = true;
            case KeyEvent.VK_D -> right = true;
            case KeyEvent.VK_SPACE -> up = true;
            case KeyEvent.VK_SHIFT -> down = true;
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        switch (e.getKeyCode()) {
            case KeyEvent.VK_W -> forward = false;
            case KeyEvent.VK_S -> backward = false;
            case KeyEvent.VK_A -> left = false;
            case KeyEvent.VK_D -> right = false;
            case KeyEvent.VK_SPACE -> up = false;
            case KeyEvent.VK_SHIFT -> down = false;
        }
    }

    @Override
    public void keyTyped(KeyEvent e) {

    }

    @Override
    public void mouseDragged(MouseEvent e) {
        if (!rotating) return;

        int dx = e.getX() - lastMouseX;
        int dy = e.getY() - lastMouseY;

        camera.rotate(-dx * mouseSensitivity, -dy * mouseSensitivity);

        lastMouseX = e.getX();
        lastMouseY = e.getY();

        component.repaint();
    }

    @Override
    public void mouseMoved(MouseEvent e) {

    }
}
