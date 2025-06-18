package cn.njit.steganography;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Random;

public class LSBSteganography {
    // 隐藏文本到BMP图像（BMP专用版本）
    public static BufferedImage hideTextInImage(String text, BufferedImage image, boolean useRandomPos, int seed) {
        // 检查是否为BMP兼容类型
        int imageType = image.getType();
        if (imageType != BufferedImage.TYPE_3BYTE_BGR &&
                imageType != BufferedImage.TYPE_INT_RGB &&
                imageType != BufferedImage.TYPE_INT_ARGB) {
            // 转换为BMP兼容类型
            BufferedImage compatibleImage = new BufferedImage(
                    image.getWidth(),
                    image.getHeight(),
                    BufferedImage.TYPE_3BYTE_BGR
            );
            compatibleImage.getGraphics().drawImage(image, 0, 0, null);
            image = compatibleImage;
        }

        byte[] textBytes = text.getBytes(StandardCharsets.UTF_8);
        byte[] message = new byte[textBytes.length + 4]; // 增加4字节存储长度信息

        // 存储文本长度
        int textLength = textBytes.length;
        message[0] = (byte) ((textLength >> 24) & 0xFF);
        message[1] = (byte) ((textLength >> 16) & 0xFF);
        message[2] = (byte) ((textLength >> 8) & 0xFF);
        message[3] = (byte) (textLength & 0xFF);

        // 复制文本内容
        System.arraycopy(textBytes, 0, message, 4, textBytes.length);

        int width = image.getWidth();
        int height = image.getHeight();
        int pixelCount = width * height;

        if (message.length * 8 > pixelCount) {
            throw new IllegalArgumentException("文本过长，无法隐藏在图像中");
        }

        Random random = new Random(seed); // 使用固定种子生成可重复的随机序列
        int[] pixelPositions = new int[message.length * 8];

        if (useRandomPos) {
            // 生成不重复的随机位置
            boolean[] usedPositions = new boolean[pixelCount];
            for (int i = 0; i < pixelPositions.length; i++) {
                int pos;
                do {
                    pos = random.nextInt(pixelCount);
                } while (usedPositions[pos]);
                usedPositions[pos] = true;
                pixelPositions[i] = pos;
            }
        } else {
            for (int i = 0; i < pixelPositions.length; i++) {
                pixelPositions[i] = i;
            }
        }

        BufferedImage resultImage = new BufferedImage(width, height, image.getType());
        resultImage.setData(image.getData());

        int bitIndex = 0;
        for (byte b : message) {
            for (int i = 0; i < 8; i++) {
                int pos = pixelPositions[bitIndex];
                int x = pos % width;
                int y = pos / width;

                int pixel = resultImage.getRGB(x, y);

                // 根据图像类型处理像素
                int alpha = 0xFF; // 默认不透明
                int red, green, blue;

                if (imageType == BufferedImage.TYPE_INT_ARGB) {
                    alpha = (pixel >> 24) & 0xff;
                    red = (pixel >> 16) & 0xff;
                    green = (pixel >> 8) & 0xff;
                    blue = pixel & 0xff;
                } else {
                    // 对于没有Alpha通道的类型
                    red = (pixel >> 16) & 0xff;
                    green = (pixel >> 8) & 0xff;
                    blue = pixel & 0xff;
                }

                // 检测是否为灰度像素
                boolean isGrayscale = (red == green && green == blue);
                int bitValue = (b >> (7 - i)) & 0x01;

                if (isGrayscale) {
                    // 对灰度像素的处理 - 只修改蓝色通道（因为RGB相同）
                    blue = (blue & 0xFE) | bitValue;
                    red = green = blue;
                } else {
                    // 对彩色像素的处理 - 使用固定的通道顺序（R,G,B循环）
                    switch (bitIndex % 3) {
                        case 0: red = (red & 0xFE) | bitValue; break;
                        case 1: green = (green & 0xFE) | bitValue; break;
                        case 2: blue = (blue & 0xFE) | bitValue; break;
                    }
                }

                // 根据图像类型设置像素
                int newPixel;
                if (imageType == BufferedImage.TYPE_INT_ARGB) {
                    newPixel = (alpha << 24) | (red << 16) | (green << 8) | blue;
                } else {
                    newPixel = (0xFF << 24) | (red << 16) | (green << 8) | blue;
                }

                resultImage.setRGB(x, y, newPixel);
                bitIndex++;
            }
        }

        return resultImage;
    }

    // 从图像中提取文本（BMP专用版本）
    public static String extractTextFromImage(BufferedImage image, boolean useRandomPos, int seed) {
        // 检查是否为BMP兼容类型
        int imageType = image.getType();
        if (imageType != BufferedImage.TYPE_3BYTE_BGR &&
                imageType != BufferedImage.TYPE_INT_RGB &&
                imageType != BufferedImage.TYPE_INT_ARGB) {
            // 转换为BMP兼容类型
            BufferedImage compatibleImage = new BufferedImage(
                    image.getWidth(),
                    image.getHeight(),
                    BufferedImage.TYPE_3BYTE_BGR
            );
            compatibleImage.getGraphics().drawImage(image, 0, 0, null);
            image = compatibleImage;
        }

        int width = image.getWidth();
        int height = image.getHeight();
        int pixelCount = width * height;
        Random random = new Random(seed); // 使用与隐藏时相同的种子

        // 读取前4字节获取文本长度
        byte[] lengthBytes = new byte[4];
        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 8; j++) {
                int bitIndex = i * 8 + j;
                int pos;

                if (useRandomPos) {
                    // 生成与隐藏时相同的随机序列
                    pos = random.nextInt(pixelCount);
                } else {
                    pos = bitIndex;
                }

                int x = pos % width;
                int y = pos / width;
                int pixel = image.getRGB(x, y);

                // 根据图像类型处理像素
                int red, green, blue;

                if (imageType == BufferedImage.TYPE_INT_ARGB) {
                    red = (pixel >> 16) & 0xff;
                    green = (pixel >> 8) & 0xff;
                    blue = pixel & 0xff;
                } else {
                    red = (pixel >> 16) & 0xff;
                    green = (pixel >> 8) & 0xff;
                    blue = pixel & 0xff;
                }

                // 检测是否为灰度像素
                boolean isGrayscale = (red == green && green == blue);
                int bitValue;

                if (isGrayscale) {
                    // 从蓝色通道提取位
                    bitValue = blue & 0x01;
                } else {
                    // 从固定的通道顺序提取位
                    switch (bitIndex % 3) {
                        case 0: bitValue = red & 0x01; break;
                        case 1: bitValue = green & 0x01; break;
                        case 2: bitValue = blue & 0x01; break;
                        default: bitValue = 0;
                    }
                }

                // 构建字节
                lengthBytes[i] = (byte) ((lengthBytes[i] << 1) | bitValue);
            }
        }

        // 计算文本长度
        int textLength =
                ((lengthBytes[0] & 0xFF) << 24) |
                        ((lengthBytes[1] & 0xFF) << 16) |
                        ((lengthBytes[2] & 0xFF) << 8) |
                        (lengthBytes[3] & 0xFF);

        // 读取文本内容
        byte[] textBytes = new byte[textLength];
        for (int i = 0; i < textLength; i++) {
            for (int j = 0; j < 8; j++) {
                int bitIndex = 32 + i * 8 + j; // 跳过长度信息的32位
                int pos;

                if (useRandomPos) {
                    pos = random.nextInt(pixelCount);
                } else {
                    pos = bitIndex;
                }

                if (pos >= pixelCount) {
                    throw new IllegalArgumentException("无法从图像中提取更多位");
                }

                int x = pos % width;
                int y = pos / width;
                int pixel = image.getRGB(x, y);

                // 根据图像类型处理像素
                int red, green, blue;

                if (imageType == BufferedImage.TYPE_INT_ARGB) {
                    red = (pixel >> 16) & 0xff;
                    green = (pixel >> 8) & 0xff;
                    blue = pixel & 0xff;
                } else {
                    red = (pixel >> 16) & 0xff;
                    green = (pixel >> 8) & 0xff;
                    blue = pixel & 0xff;
                }

                // 检测是否为灰度像素
                boolean isGrayscale = (red == green && green == blue);
                int bitValue;

                if (isGrayscale) {
                    bitValue = blue & 0x01;
                } else {
                    switch (bitIndex % 3) {
                        case 0: bitValue = red & 0x01; break;
                        case 1: bitValue = green & 0x01; break;
                        case 2: bitValue = blue & 0x01; break;
                        default: bitValue = 0;
                    }
                }

                // 构建字节
                textBytes[i] = (byte) ((textBytes[i] << 1) | bitValue);
            }
        }

        return new String(textBytes, StandardCharsets.UTF_8);
    }

    // 保存图像为BMP格式
    public static void saveAsBMP(BufferedImage image, String filePath) throws IOException {
        File output = new File(filePath);
        ImageIO.write(image, "BMP", output);
    }

    // 从BMP文件加载图像
    public static BufferedImage loadBMP(String filePath) throws IOException {
        File input = new File(filePath);
        return ImageIO.read(input);
    }
}
