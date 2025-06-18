package cn.njit.util;

import java.io.*;
import java.util.zip.CRC32;

public class CRC32Util {
    // 计算字符串的CRC32值
    public static long calculate(String data) {
        CRC32 crc32 = new CRC32();
        crc32.update(data.getBytes());
        return crc32.getValue();
    }

    // 计算文件的CRC32值
    public static long calculateFile(String filePath) throws Exception {
        CRC32 crc32 = new CRC32();
        byte[] buffer = new byte[8192];
        try (FileInputStream fis = new FileInputStream(filePath);
             BufferedInputStream bis = new BufferedInputStream(fis)) {
            int len;
            while ((len = bis.read(buffer)) != -1) {
                crc32.update(buffer, 0, len);
            }
        }
        return crc32.getValue();
    }

    // 测试方法
    public static void main(String[] args) {
        String data = "Hello, CRC32!";
        long crc = calculate(data);
        System.out.println("CRC32值：" + crc);

        try {
            long fileCrc = calculateFile("test.txt");
            System.out.println("文件CRC32值：" + fileCrc);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
