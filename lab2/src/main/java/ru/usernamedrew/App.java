package ru.usernamedrew;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public class App {
    public static void main( String[] args ) {
        try {
            File file = new File("./src/main/resources/images/star.png");
            BufferedImage image = ImageIO.read(file);

            if (image != null) {
                int width = image.getWidth(null);
                int height = image.getHeight(null);

                for (int y = 0; y < height; y++) {
                    for (int x = 0; x < width; x++) {
                        int pixel = image.getRGB(x, y);
                        Color c = new Color(pixel);
                        int r = c.getRed();
                        int g = c.getGreen();
                        int b = c.getBlue();
                        System.out.println(r + "," + g + "," + b);
                    }
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
