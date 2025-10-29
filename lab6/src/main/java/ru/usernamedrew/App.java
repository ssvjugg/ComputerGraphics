package ru.usernamedrew;

import ru.usernamedrew.ui.MainFrame;

import javax.swing.*;

public class App {
    public static void main( String[] args ) {
        SwingUtilities.invokeLater(() -> {
            new MainFrame().setVisible(true);
        });
    }
}
