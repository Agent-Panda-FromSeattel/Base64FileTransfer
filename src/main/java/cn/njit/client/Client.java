package cn.njit.client;

import cn.njit.base64.Base64Util;


import java.io.*;
import java.net.Socket;
import java.util.Scanner;

public class Client {
    private static final String SERVER_HOST = "localhost";
    private static final int SERVER_PORT = 8888;
    private Socket socket;

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
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // 添加发送方法
    public void sendData(String data) throws IOException {
        String encodedData = Base64Util.encode(data);
        socket.getOutputStream().write(encodedData.getBytes());
        socket.getOutputStream().flush();
    }

    private UserInteractionThread interactionThread;
    private WorkThread workThread;

    public void start() {
        if (connect()) {
            // 启动用户交互线程
            interactionThread = new UserInteractionThread(this);
            interactionThread.start();

            // 启动工作线程（如文件传输）
            workThread = new WorkThread(this);
            workThread.start();
        }
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
            System.out.println("请输入消息（输入'exit'退出）：");
            while (true) {
                String message = scanner.nextLine();
                if ("exit".equals(message)) {
                    client.disconnect();
                    break;
                }
                try {
                    client.sendData(message);
                } catch (IOException e) {
                    e.printStackTrace();
                    break;
                }
            }
            scanner.close();
        }
    }

    // 工作线程（示例：文件传输）
    private static class WorkThread extends Thread {
        private Client client;

        public WorkThread(Client client) {
            this.client = client;
        }

        @Override
        public void run() {
            try {
                // 模拟文件传输
                String filePath = "src/test/resources/test.txt"; // 资源目录下的文件

                // 检查文件是否存在
                File file = new File(filePath);
                if (!file.exists()) {
                    System.out.println("文件不存在: " + filePath);
                    return;
                }
                String encodedFile = Base64Util.encodeFile(filePath);
                // 发送文件数据
                client.sendData(encodedFile);
                System.out.println("文件传输完成");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    // main方法修改为启动多线程
    public static void main(String[] args) {
        Client client = new Client();
        client.start();
    }
}
