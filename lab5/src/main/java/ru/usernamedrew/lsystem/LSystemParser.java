package ru.usernamedrew.lsystem;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

//Читает файлы вида:
//F 60 0        ← атом, угол поворота, начальное направление
//F->F-F++F-F   ← правило замены
public class LSystemParser {

    public static LSystem parseFromFile(String filename) throws IOException {
        LSystem lsystem = new LSystem();

        try (BufferedReader br = new BufferedReader(new FileReader(filename))) {
            String firstLine = br.readLine();
            if (firstLine == null) {
                throw new IOException("Файл пуст");
            }

            String[] firstParts = firstLine.split(" ");
            if (firstParts.length < 3) {
                throw new IOException("Неверный формат первой строки");
            }

            char atom = firstParts[0].charAt(0);
            double angle = Double.parseDouble(firstParts[1]);
            double initialAngle = Double.parseDouble(firstParts[2]);

            lsystem.initialize(String.valueOf(atom), angle, initialAngle);

            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (!line.isEmpty()) {
                    String[] parts = line.split("->");
                    if (parts.length == 2) {
                        char key = parts[0].trim().charAt(0);
                        String value = parts[1].trim();
                        lsystem.addRule(key, value);
                    }
                }
            }
        }

        return lsystem;
    }
}