package cn.njit.base64;

import java.util.Base64;

public class Base64Util {
    // 编码方法
    public static String encode(String text) {
        return Base64.getEncoder().encodeToString(text.getBytes());
    }

    // 编码文件
    public static String encodeFile(String filePath) throws Exception {
        byte[] data = java.nio.file.Files.readAllBytes(java.nio.file.Paths.get(filePath));
        return Base64.getEncoder().encodeToString(data);
    }

    // 解码方法
    public static String decode(String base64Text) {
        byte[] decoded = Base64.getDecoder().decode(base64Text);
        return new String(decoded);
    }

    // 解码文件
    public static void decodeFile(String base64Text, String outputPath) throws Exception {
        byte[] decoded = Base64.getDecoder().decode(base64Text);
        java.nio.file.Files.write(java.nio.file.Paths.get(outputPath), decoded);
    }

    // 测试方法
    public static void main(String[] args) {
        String original = "Hello, BASE64!";
        String encoded = encode(original);
        String decoded = decode(encoded);
        System.out.println("原始数据：" + original);
        System.out.println("编码后：" + encoded);
        System.out.println("解码后：" + decoded);
    }
}
