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

    public void start() {
        if (connect()) {
            // 启动用户交互线程
            interactionThread = new UserInteractionThread(this);
            interactionThread.start();
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

    // main方法修改为启动多线程
    public static void main(String[] args) {
        Client client = new Client();
        client.start();
    }
}
