package ru.usernamedrew.tasks;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

// запустить:
//cd lab2
//javac -d bin src/main/java/ru/usernamedrew/tasks/Histogram.java
//java -cp bin ru.usernamedrew.tasks.Histogram

public class Histogram {

    public static void main(String[] args) throws IOException {
        File file = new File("./src/main/resources/images/dab.png");

        BufferedImage image = ImageIO.read(file);

        int width = image.getWidth();
        int height = image.getHeight();

        // массивы для гистограмм
        int[] red = new int[256];
        int[] green = new int[256];
        int[] blue = new int[256];

        // изображения для каналов
        BufferedImage redImg = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        BufferedImage greenImg = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        BufferedImage blueImg = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int rgb = image.getRGB(x, y);

                int r = (rgb >> 16) & 0xFF;
                int g = (rgb >> 8) & 0xFF;
                int b = rgb & 0xFF;

                // заполняем гистограммы
                red[r]++;
                green[g]++;
                blue[b]++;

                // создаем каналы
                redImg.setRGB(x, y, (r << 16));
                greenImg.setRGB(x, y, (g << 8));
                blueImg.setRGB(x, y, b);
            }
        }

        // lab2/results_lab_2_histograms
        File resultsDir = new File("results_lab_2_histograms");
        if (!resultsDir.exists())
            resultsDir.mkdirs();

        File redFile = new File(resultsDir, "red.png");
        File greenFile = new File(resultsDir, "green.png");
        File blueFile = new File(resultsDir, "blue.png");

        ImageIO.write(redImg, "png", redFile);
        ImageIO.write(greenImg, "png", greenFile);
        ImageIO.write(blueImg, "png", blueFile);

        // вывод гистограмм
        GreyShades.displayChart("Красный", red, GreyShades.findMaxCount(red), "Канал красного цвета", "Частота");

        GreyShades.displayChart("Зеленый", red, GreyShades.findMaxCount(green), "Канал зеленого цвета", "Частота");

        GreyShades.displayChart("Синий", red, GreyShades.findMaxCount(blue), "Канал синего цвета", "Частота");
    }

}