package cn.njit.client;

import cn.njit.base64.Base64Util;


import java.io.*;
import java.net.Socket;
import java.util.Scanner;

public class Client {
    private static final String SERVER_HOST = "localhost";
    private static final int SERVER_PORT = 8888;
    private Socket socket;
    private final Object outputLock = new Object(); // 用于同步输出流操作

    public boolean connect() {
        try {
            socket = new Socket(SERVER_HOST, SERVER_PORT);
            System.out.println("客户端连接到服务端：" + SERVER_HOST + ":" + SERVER_PORT);
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }


    public void disconnect() {
        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
                System.out.println("连接已关闭");
            }
        } catch (IOException e) {
            System.err.println("关闭连接时出错: " + e.getMessage());
        }
    }

    public boolean isConnected() {
        return socket != null && !socket.isClosed() && socket.isConnected();
    }

    // 新增方法：检查文件是否存在并打印调试信息
    private boolean checkFileExists(String filePath) {
        File file = new File(filePath);
        boolean exists = file.exists();
        System.out.println("检查文件路径: " + filePath);
        System.out.println("绝对路径: " + file.getAbsolutePath());
        System.out.println("文件是否存在: " + exists);
        return exists;
    }

    // 修改后的sendData方法
    public synchronized void sendData(String data) throws IOException {
        if (!isConnected()) {
            throw new IOException("连接未建立或已关闭");
        }
        String encodedData = Base64Util.encode(data);
        OutputStream out = socket.getOutputStream();
        out.write(encodedData.getBytes());
        out.flush();
    }

    // 发送普通消息（自动添加MSG协议头）
    public void sendMessage(String message) throws IOException {
        sendData("MSG:" + message);
    }

    // 修正后的文件发送方法
    public void sendFile(String filePath) {
        new Thread(() -> {
            try {
                if (!checkFileExists(filePath)) {
                    System.err.println("错误：文件不存在或路径无效！");
                    return;
                }
                File file = new File(filePath);
                String fileName = file.getName();
                String encodedFile = Base64Util.encodeFile(filePath);
                sendData("FILE:" + fileName + "|" + encodedFile);
                System.out.println("文件已发送: " + fileName);
            } catch (Exception e) {
                System.err.println("文件传输失败: " + e.getMessage());
                e.printStackTrace();
            }
        }).start();
    }

    // 用户交互线程
    private static class UserInteractionThread extends Thread {
        private Client client;
        private Scanner scanner = new Scanner(System.in);

        public UserInteractionThread(Client client) {
            this.client = client;
        }


        @Override
        public void run() {
            System.out.println("输入消息（exit退出，upload <文件路径> 上传文件）:");
            System.out.println("当前工作目录: " + System.getProperty("user.dir")); // 打印工作目录
            while (true) {
                String input = scanner.nextLine();
                if ("exit".equalsIgnoreCase(input)) {
                    client.disconnect();
                    break;
                } else if (input.startsWith("upload ")) {
                    // 修复点：精确提取 < > 内的路径
                    int start = input.indexOf('<');
                    int end = input.indexOf('>');
                    if (start == -1 || end == -1 || start >= end) {
                        System.err.println("错误：请使用格式 upload <文件路径>");
                        continue;
                    }
                    String filePath = input.substring(start + 1, end).trim();
                    client.sendFile(filePath);
                } else {
                    try {
                        client.sendMessage(input); // 发送普通消息
                    } catch (IOException e) {
                        System.err.println("发送失败: " + e.getMessage());
                        break;
                    }
                }
            }
            scanner.close();
        }
    }

    public void start() {
        if (connect()) {
            new UserInteractionThread(this).start(); // 启动用户交互线程
        }
    }

    public static void main(String[] args) {
        new Client().start();
    }
}
