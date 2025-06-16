package cn.njit.server;

import cn.njit.base64.Base64Util;
import cn.njit.db.SQLiteDB;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class Server {
    private static final int PORT = 8888;
    private ServerSocket serverSocket;
    private volatile boolean running = false; // 使用volatile保证可见性
    private static final String CURRENT_VERSION = "1.0.0";
    private static boolean upgradeFlag = false;
    private ExecutorService threadPool = Executors.newFixedThreadPool(10);
    private SQLiteDB database;
    private static final String UPLOAD_DIR = "uploads";

    public Server() {
        database = new SQLiteDB();
        database.connect();

        try {
            Files.createDirectories(Paths.get(UPLOAD_DIR));
        } catch (IOException e) {
            System.err.println("无法创建上传目录: " + e.getMessage());
        }
    }

    public void start() {
        try {
            serverSocket = new ServerSocket(PORT);
            running = true;
            System.out.println("服务端启动, listening on port:" + PORT);

            // 使用单独线程处理客户端连接
            Thread acceptThread = new Thread(() -> {
                while (running) {
                    try {
                        Socket clientSocket = serverSocket.accept();
                        System.out.println("客户端连接:" + clientSocket.getInetAddress());
                        threadPool.execute(new ClientHandler(clientSocket));
                    } catch (IOException e) {
                        if (running) {
                            System.err.println("接受连接错误: " + e.getMessage());
                        }
                    }
                }
            });
            acceptThread.start();

            // 处理控制台输入
            handleConsoleInput();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void handleConsoleInput() {
        Scanner scanner = new Scanner(System.in);
        System.out.println("输入 'stop' 停止服务端:");
        while (running) {
            String input = scanner.nextLine();
            if ("stop".equalsIgnoreCase(input)) {
                stop();
                break;
            }
        }
        scanner.close();
    }

    public void stop() {
        running = false;
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (IOException e) {
            System.err.println("关闭ServerSocket错误: " + e.getMessage());
        }

        // 优雅关闭线程池
        threadPool.shutdown();
        try {
            if (!threadPool.awaitTermination(5, TimeUnit.SECONDS)) {
                threadPool.shutdownNow();
            }
        } catch (InterruptedException e) {
            threadPool.shutdownNow();
            Thread.currentThread().interrupt();
        }

        database.disconnect();
        System.out.println("服务端已停止");
    }

    private class ClientHandler implements Runnable {
        private final Socket clientSocket;
        private volatile boolean clientConnected = true;

        public ClientHandler(Socket socket) {
            this.clientSocket = socket;
        }

        @Override
        public void run() {
            try (InputStream in = clientSocket.getInputStream();
                 OutputStream out = clientSocket.getOutputStream()) {

                // 增加文件传输超时时间
                clientSocket.setSoTimeout(120000); // 120秒超时

                BufferedReader reader = new BufferedReader(new InputStreamReader(in));
                PrintWriter writer = new PrintWriter(out, true);

                // 发送欢迎消息
                writer.println(Base64Util.encode("SERVER:欢迎连接到服务器"));

                // 文件传输相关变量
                String fileName = null;
                StringBuilder fileContent = new StringBuilder();

                // 持续处理客户端消息
                while (clientConnected && running) {
                    try {
                        String encodedData = reader.readLine();
                        if (encodedData == null) {
                            // 客户端关闭连接
                            break;
                        }

                        String decodedData = Base64Util.decode(encodedData);
                        System.out.println("接收数据：" + decodedData);

                        if ("exit".equalsIgnoreCase(decodedData)) {
                            writer.println(Base64Util.encode("SERVER:连接即将关闭"));
                            break;
                        } else if (decodedData.startsWith("MSG:")) {
                            String message = decodedData.substring(4);
                            System.out.println("文本消息: " + message);
                            // 发送回执
                            writer.println(Base64Util.encode("SERVER:消息已接收"));
                        } else if (decodedData.startsWith("FILE_START:")) {
                            // 开始接收文件
                            fileName = decodedData.substring(11);
                            fileContent.setLength(0);
                            writer.println(Base64Util.encode("SERVER:开始接收文件"));
                        } else if (decodedData.equals("FILE_END")) {
                            // 结束接收文件
                            if (fileName != null) {
                                handleFileUpload(fileName, fileContent.toString());
                                writer.println(Base64Util.encode("SERVER:文件已接收"));
                                fileName = null;
                            }
                        } else if (fileName != null) {
                            // 文件内容块
                            fileContent.append(decodedData);
                            writer.println(Base64Util.encode("SERVER:文件块已接收"));
                        } else {
                            writer.println(Base64Util.encode("SERVER:未知命令"));
                        }
                    } catch (IOException e) {
                        if (!clientSocket.isClosed()) {
                            System.out.println("读取数据错误: " + e.getMessage());
                        }
                        break;
                    }
                }

            } catch (IOException e) {
                System.out.println("客户端连接错误: " + e.getMessage());
            } finally {
                try {
                    clientSocket.close();
                    System.out.println("客户端连接关闭: " + clientSocket.getInetAddress());
                } catch (IOException e) {
                    System.err.println("关闭客户端socket错误: " + e.getMessage());
                }
            }
        }

        private void handleFileUpload(String fileName, String encodedFileData) {
            try {
                Path filePath = Paths.get(UPLOAD_DIR, fileName);
                Base64Util.decodeFile(encodedFileData, filePath.toString());
                System.out.println("文件保存成功: " + filePath);

                long recordId = database.insertFile(fileName, "上传自客户端");
                System.out.println("文件记录已存入数据库，ID: " + recordId);

            } catch (Exception e) {
                System.err.println("处理文件上传失败: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args) {
        Server server = new Server();
        server.start();
    }
}