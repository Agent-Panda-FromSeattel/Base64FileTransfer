package cn.njit.server;

import cn.njit.base64.Base64Util;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server {
    private static final int PORT = 8888;
    private ServerSocket serverSocket;
    private boolean running = false;
    // 使用线程池
    private ExecutorService threadPool = Executors.newFixedThreadPool(10);

    public void start() {
        try {
            serverSocket = new ServerSocket(PORT);
            running = true;
            System.out.println("服务端启动, listening on port:" + PORT);

            // 启动一个线程来处理客户端连接
            new Thread(() -> {
                while (running) {
                    try {
                        Socket clientSocket = serverSocket.accept();
                        System.out.println("客户端连接:" + clientSocket.getInetAddress());
                        // 为每个客户端创建一个新线程处理
                        threadPool.execute(new ClientHandler(clientSocket));
                    } catch (IOException e) {
                        if (running) {
                            e.printStackTrace();
                        }
                    }
                }
            }).start();

            // 主线程用于和用户交互
            Scanner scanner = new Scanner(System.in);
            System.out.println("输入 'stop' 停止服务端:");
            while (running) {
                String input = scanner.nextLine();
                if ("stop".equals(input)) {
                    stop();
                }
            }
            scanner.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void stop() {
        running = false;
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        threadPool.shutdown(); // 关闭线程池
    }

    // 内部类：处理客户端连接
    private static class ClientHandler extends Thread {
        private Socket clientSocket;

        public ClientHandler(Socket socket) {
            this.clientSocket = socket;
        }

        @Override
        public void run() {
            try (var in = clientSocket.getInputStream();
                 var out = clientSocket.getOutputStream()) {

                byte[] buffer = new byte[1024];
                int length;

                // 循环读取客户端数据（直到连接关闭）
                while ((length = in.read(buffer)) != -1) {
                    String encodedData = new String(buffer, 0, length);
                    String decodedData = Base64Util.decode(encodedData);
                    System.out.println("接收数据：" + decodedData);
                }
            } catch (IOException e) {
                System.out.println("客户端断开连接: " + clientSocket.getInetAddress());
            } finally {
                try {
                    clientSocket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public static void main(String[] args) {
        Server server = new Server();
        server.start();
    }
}
