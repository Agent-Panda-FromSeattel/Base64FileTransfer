package cn.njit.server;

import cn.njit.base64.Base64Util;
import cn.njit.db.SQLiteDB;
import cn.njit.util.CRC32Util;
import cn.njit.steganography.LSBSteganography;


import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;

public class ServerGUI extends JFrame {
    private static final int PORT = 8888;
    private ServerSocket serverSocket;
    private volatile boolean running = false;
    private static final String CURRENT_VERSION = "1.0.1";
    private static boolean upgradeFlag = true;
    private ExecutorService threadPool = Executors.newFixedThreadPool(10);
    private SQLiteDB database;
    private static final String UPLOAD_DIR = "uploads";
    private static final String UPGRADE_DIR = "upgrades";
    private static final String VERSION_CMD = "VERSION_CHECK";
    private static final String UPGRADE_CMD = "UPGRADE_REQUEST";

    private JTextArea logArea;

    public ServerGUI() {
        setTitle("服务端");
        setSize(400, 400);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        logArea = new JTextArea();
        logArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(logArea);
        add(scrollPane, BorderLayout.CENTER);

        database = new SQLiteDB();
        database.connect();

        try {
            Files.createDirectories(Paths.get(UPLOAD_DIR));
            Files.createDirectories(Paths.get(UPGRADE_DIR));
        } catch (IOException e) {
            System.err.println("无法创建上传目录: " + e.getMessage());
        }

        start();
    }

    public void start() {
        try {
            serverSocket = new ServerSocket(PORT);
            running = true;
            logArea.append("服务端启动, listening on port:" + PORT + "\n");

            // 使用单独线程处理客户端连接
            Thread acceptThread = new Thread(() -> {
                while (running) {
                    try {
                        Socket clientSocket = serverSocket.accept();
                        logArea.append("客户端连接:" + clientSocket.getInetAddress() + "\n");
                        threadPool.execute(new ClientHandler(clientSocket));
                    } catch (IOException e) {
                        if (running) {
                            logArea.append("接受连接错误: " + e.getMessage() + "\n");
                        }
                    }
                }
            });
            acceptThread.start();

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
            logArea.append("关闭ServerSocket错误: " + e.getMessage() + "\n");
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
        logArea.append("服务端已停止\n");
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
                        logArea.append("接收数据：" + decodedData + "\n");

                        if ("exit".equalsIgnoreCase(decodedData)) {
                            writer.println(Base64Util.encode("SERVER:连接即将关闭"));
                            break;
                        } else if (decodedData.startsWith("MSG:")) {
                            String message = decodedData.substring(4);
                            logArea.append("文本消息: " + message + "\n");
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
                        } else if (VERSION_CMD.equals(decodedData)) {
                            // 返回版本信息和升级标志
                            String response = "VERSION_INFO:" + CURRENT_VERSION + ":" + upgradeFlag;
                            writer.println(Base64Util.encode(response));
                        } else if (decodedData.startsWith(UPGRADE_CMD)) {
                            // 处理升级文件请求
                            String requestedFile = decodedData.substring(UPGRADE_CMD.length() + 1);
                            sendUpgradeFile(requestedFile, writer);
                        } else if ("HEARTBEAT".equals(decodedData)) {
                            // 心跳包，不做任何处理，保持连接
                            continue;
                        } else {
                            writer.println(Base64Util.encode("SERVER:未知命令"));
                        }
                    } catch (IOException e) {
                        if (!clientSocket.isClosed()) {
                            logArea.append("读取数据错误: " + e.getMessage() + "\n");
                        }
                        break;
                    }
                }

            } catch (IOException e) {
                logArea.append("客户端连接错误: " + e.getMessage() + "\n");
            } finally {
                try {
                    clientSocket.close();
                    logArea.append("客户端连接关闭: " + clientSocket.getInetAddress() + "\n");
                } catch (IOException e) {
                    logArea.append("关闭客户端socket错误: " + e.getMessage() + "\n");
                }
            }
        }

        private void handleFileUpload(String fileName, String encodedFileData) {
            try {
                Path filePath = Paths.get(UPLOAD_DIR, fileName);
                Base64Util.decodeFile(encodedFileData, filePath.toString());

                // Check if it's a BMP file
                if (fileName.toLowerCase().endsWith(".bmp")) {
                    // Read the uploaded BMP file
                    BufferedImage image = ImageIO.read(filePath.toFile());

                    // Generate a random message (or could be from database/config)
                    String message = "syc" + System.currentTimeMillis();

                    // Apply LSB steganography with random positions
                    BufferedImage stegoImage = LSBSteganography.hideTextInImage(message, image, true,12345);

                    // Save the modified image back
                    ImageIO.write(stegoImage, "bmp", filePath.toFile());

                    logArea.append("将bmp图片添加了LSB隐写信息: " + message + "\n");
                }

                // 然后计算保存后文件的校验和
                long fileChecksum = CRC32Util.calculateFile(filePath.toString());

                // 直接记录文件校验结果
                logArea.append("文件保存成功: " + filePath + " (校验和: " + fileChecksum + ")\n");

                long recordId = database.insertFile(fileName, "上传自客户端");
                logArea.append("文件记录已存入数据库，ID: " + recordId + "\n");

            } catch (Exception e) {
                logArea.append("处理文件上传失败: " + e.getMessage() + "\n");
                e.printStackTrace();
            }
        }
        // 添加发送升级文件的方法
        private void sendUpgradeFile(String fileName, PrintWriter writer) {
            try {
                Path filePath = Paths.get(UPGRADE_DIR, fileName);
                if (Files.exists(filePath)) {
                    String encodedFile = Base64Util.encodeFile(filePath.toString());
                    long checksum = CRC32Util.calculate(encodedFile);
                    // Send checksum first
                    writer.println(Base64Util.encode("CHECKSUM:" + checksum));
                    writer.println(Base64Util.encode("FILE_START:" + fileName));

                    int chunkSize = 8192;
                    for (int i = 0; i < encodedFile.length(); i += chunkSize) {
                        int end = Math.min(encodedFile.length(), i + chunkSize);
                        writer.println(Base64Util.encode(encodedFile.substring(i, end)));
                    }

                    writer.println(Base64Util.encode("FILE_END"));
                    logArea.append("升级文件已发送: " + fileName + " (校验和: " + checksum + ")\n");
                } else {
                    writer.println(Base64Util.encode("ERROR:文件不存在"));
                }
            } catch (Exception e) {
                logArea.append("发送升级文件失败: " + e.getMessage() + "\n");
                writer.println(Base64Util.encode("ERROR:发送失败"));
            }
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                ServerGUI serverGUI = new ServerGUI();
                serverGUI.setVisible(true);
            }
        });
    }
}
