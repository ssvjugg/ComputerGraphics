package ru.usernamedrew;

import ru.usernamedrew.util.PolygonEditor;

import javax.swing.*;

public class App {
    public static void main( String[] args ) {
        SwingUtilities.invokeLater(() -> {
            new PolygonEditor().setVisible(true);
        });
    }
}
