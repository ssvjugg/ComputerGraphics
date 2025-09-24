package ru.usernamedrew.tasks;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.ImageIO;

import org.knowm.xchart.SwingWrapper;
import org.knowm.xchart.XYChart;
import org.knowm.xchart.XYChartBuilder;

public class GreyShades {
    ///Класс для хранения RGB
    static class RGB {
        public final int r;
        public final int g;
        public final int b;

        RGB(int r, int g, int b) {
            this.r = r;
            this.g = g;
            this.b = b;
        }
    }

    ///Метод для получения RGB из пикселя
    static RGB getRGBFromPixel(int pixel) {
        Color color = new Color(pixel);
        return new RGB(color.getRed(), color.getGreen(), color.getBlue());
    }

    ///Метод для получения серого цвета из RGB v1
    static int pixelToGreyscale(RGB rgb) {
        return (int) (0.299 * rgb.r + 0.587 * rgb.g + 0.114 * rgb.b);
    }

    ///Метод для получения серого цвета из RGB v2
    static int pixelToGreyscaleV2(RGB rgb) {
        return (int) (0.2126 * rgb.r + 0.7152 * rgb.g + 0.0722 * rgb.b);
    }

    ///Метод для получения разницы между двумя методами преобразования в серый цвет
    static int diffGreyscale(RGB rgb) {
        return Math.abs(pixelToGreyscale(rgb) - pixelToGreyscaleV2(rgb));
    }

    ///Метод для вычисления гистограммы
    static int[] calculateHistogram(List<Integer> intensities) {
        int[] histogram = new int[256];

        for (int intensity : intensities) {
            if (intensity >= 0 && intensity <= 255) {
                histogram[intensity]++;
            }
        }

        return histogram;
    }

    ///Метод для поиска максимального значения в гистограмме
    public static int findMaxCount(int[] histogram) {
        int maxCount = 0;
        for (int count : histogram) {
            if (count > maxCount) {
                maxCount = count;
            }
        }
        return maxCount;
    }

    ///Метод для преобразования массива int в массив double
    public static double[] toDoubleArray(int[] data) {
        double[] doubleData = new double[data.length];
        for (int i = 0; i < data.length; i++) {
            doubleData[i] = data[i];
        }
        return doubleData;
    }

    ///Метод для отображения гистограммы
    public static void displayChart(String title, int[] histogramData, int overallMaxCount, String xAxisTitle, String yAxisTitle) {
        XYChart chart = new XYChartBuilder().width(800).height(600).title(title).xAxisTitle(xAxisTitle).yAxisTitle(yAxisTitle).build();
        chart.getStyler().setYAxisMin(0.0);
        chart.getStyler().setYAxisMax((double) overallMaxCount * 1.1);
        chart.addSeries("Интенсивность", null, toDoubleArray(histogramData));
        new SwingWrapper(chart).displayChart();
    }

    ///Метод для конвертирования изображения в серый цвет и постоения гистограмм
    static void convertImageToGreyscale(String inputImagePath) {
        try {
            File file = new File(inputImagePath);
            BufferedImage image = ImageIO.read(file);

            if (image == null) {
                System.out.println("Не получилось открыть файл");
                return;
            }

            BufferedImage newImage1 = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_BYTE_GRAY);
            WritableRaster raster1 = newImage1.getRaster();

            BufferedImage newImage2 = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_BYTE_GRAY);
            WritableRaster raster2 = newImage2.getRaster();

            BufferedImage newImage3 = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_BYTE_GRAY);
            WritableRaster raster3 = newImage3.getRaster();

            List<Integer> intensities1 = new ArrayList<>();
            List<Integer> intensities2 = new ArrayList<>();

            //Преобразование изображений
            for (int y = 0; y < image.getHeight(); y++) {
                for (int x = 0; x < image.getWidth(); x++) {
                    int pixel = image.getRGB(x, y);
                    RGB rgb = getRGBFromPixel(pixel);

                    int grey1 = pixelToGreyscale(rgb);
                    raster1.setSample(x, y, 0, grey1);
                    intensities1.add(grey1);

                    int grey2 = pixelToGreyscaleV2(rgb);
                    raster2.setSample(x, y, 0, grey2);
                    intensities2.add(grey2);

                    raster3.setSample(x, y, 0, diffGreyscale(rgb));
                }
            }

            //Сохранение изображений
            ImageIO.write(newImage1, "jpg", new File(inputImagePath.replace(".jpg", "_v1.jpg")));
            ImageIO.write(newImage2, "jpg", new File(inputImagePath.replace(".jpg", "_v2.jpg")));
            ImageIO.write(newImage3, "jpg", new File(inputImagePath.replace(".jpg", "_diff.jpg")));


            // Построение гистограмм
            int[] histogram1 = calculateHistogram(intensities1);
            int[] histogram2 = calculateHistogram(intensities2);

            int maxCount1 = findMaxCount(histogram1);
            int maxCount2 = findMaxCount(histogram2);

            int overallMaxCount = Math.max(maxCount1, maxCount2);

            displayChart("Гистограмма 1", histogram1, overallMaxCount, "Интенсивность", "Кол-во пикселей");
            displayChart("Гистограмма 2", histogram2, overallMaxCount, "Интенсивность", "Кол-во пикселей");

        } catch (Exception e) {
            System.err.println("Ошибка: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        convertImageToGreyscale("src/main/resources/images/dab.png");
    }
}