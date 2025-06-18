package cn.njit.client;

import cn.njit.base64.Base64Util;
import cn.njit.util.CRC32Util;
import cn.njit.steganography.LSBSteganography;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;

public class ClientGUI_v2 extends JFrame {
    private static final String SERVER_HOST = "localhost";
    private static final int SERVER_PORT = 8888;
    private static final String CLIENT_VERSION = "1.0.1";
    private static final String CLIENT_JAR = "ClientGUI.jar";
    private static final String TEMP_CLIENT_JAR = "ClientGUI.temp.jar";
    private Socket socket;
    private PrintWriter writer;
    private BufferedReader reader;

    private JTextArea messageArea;
    private JTextField inputField;
    private JButton sendButton;
    private JButton fileButton;

    public ClientGUI_v2() {
        setTitle("客户端（新版本 v" + CLIENT_VERSION + "）"); // 标题注明新版本
        setSize(400, 400);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        messageArea = new JTextArea();
        messageArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(messageArea);
        add(scrollPane, BorderLayout.CENTER);

        JPanel inputPanel = new JPanel();
        inputPanel.setLayout(new BorderLayout());
        inputField = new JTextField();
        sendButton = new JButton("发送");
        fileButton = new JButton("选择文件");
        inputPanel.add(inputField, BorderLayout.CENTER);
        inputPanel.add(sendButton, BorderLayout.EAST);
        inputPanel.add(fileButton, BorderLayout.WEST);
        add(inputPanel, BorderLayout.SOUTH);
        // 添加新功能提示
        JButton newFeatureButton = new JButton("点击查看新功能");
        newFeatureButton.addActionListener(e -> {
            JOptionPane.showMessageDialog(this,
                    "这是新版本 v" + CLIENT_VERSION + " 新增的功能！\n" +
                            "1. 数据传输添加了CRC32校验\n" +
                            "2. 上传bmp文件时会使用LSB隐写技术把文字信息隐藏进图像文件");
        });
        add(newFeatureButton, BorderLayout.NORTH);

        sendButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String message = inputField.getText();
                if (!message.isEmpty()) {
                    try {
                        sendMessage(message);
                        inputField.setText("");
                    } catch (IOException ex) {
                        ex.printStackTrace();
                    }
                }
            }
        });

        fileButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JFileChooser fileChooser = new JFileChooser();
                int result = fileChooser.showOpenDialog(ClientGUI_v2.this);
                if (result == JFileChooser.APPROVE_OPTION) {
                    File selectedFile = fileChooser.getSelectedFile();
                    sendFile(selectedFile.getAbsolutePath());
                }
                // After sending, check if we received any files recently
                File downloadsDir = new File(System.getProperty("user.dir"));
                File[] downloadedFiles = downloadsDir.listFiles((dir, name) ->
                        name.toLowerCase().endsWith(".bmp"));

                if (downloadedFiles != null) {
                    for (File file : downloadedFiles) {
                        checkForSteganography(file);
                    }
                }
            }
        });

        connect();
    }
    private void checkForSteganography(File file) {
        try {
            if (file.getName().toLowerCase().endsWith(".bmp")) {
                BufferedImage image = ImageIO.read(file);

                try {
                    // Try to extract hidden message (with random position flag matching server)
                    String hiddenMessage = LSBSteganography.extractTextFromImage(image, true,12345);
                    messageArea.append("发现隐写信息: " + hiddenMessage + "\n");
                } catch (IllegalArgumentException e) {
                    messageArea.append("未检测到隐写信息: " + e.getMessage() + "\n");
                }
            }
        } catch (IOException e) {
            messageArea.append("检查隐写信息时出错: " + e.getMessage() + "\n");
        }
    }
    private void checkVersion() throws IOException {
        System.out.println("开始执行 checkVersion 方法");
        writer.println(Base64Util.encode("VERSION_CHECK"));
        String response = Base64Util.decode(reader.readLine());
        System.out.println("接收到的版本信息: " + response); // 添加日志输出

        if (response.startsWith("VERSION_INFO:")) {
            String[] parts = response.split(":");
            String serverVersion = parts[1];
            boolean serverUpgradeFlag = Boolean.parseBoolean(parts[2]);

            System.out.println("服务端版本号: " + serverVersion); // 添加日志输出
            System.out.println("服务端升级标志: " + serverUpgradeFlag); // 添加日志输出

            if (serverUpgradeFlag && !serverVersion.equals(CLIENT_VERSION)) {
                int choice = JOptionPane.showConfirmDialog(this,
                        "发现新版本: " + serverVersion + "\n是否立即升级?",
                        "版本升级", JOptionPane.YES_NO_OPTION);

                if (choice == JOptionPane.YES_OPTION) {
                    messageArea.append("发现新版本: " + serverVersion + "\n");
                    messageArea.append("正在下载新版本...\n");

                    // 请求升级文件
                    writer.println(Base64Util.encode("UPGRADE_REQUEST:" + CLIENT_JAR));

                    // 接收文件
                    receiveUpgradeFile();

                    messageArea.append("新版本下载完成，即将重启应用进行升级\n");

                    // 创建更新脚本并执行
                    createUpdateScript();
                    executeUpdateScript();
                }
            }
        }
    }
    public boolean connect() {
        try {
            socket = new Socket();
            System.out.println("尝试连接到服务端：" + SERVER_HOST + ":" + SERVER_PORT);
            // 设置连接超时5秒
            socket.connect(new InetSocketAddress(SERVER_HOST, SERVER_PORT), 5000);
            System.out.println("客户端成功连接到服务端：" + SERVER_HOST + ":" + SERVER_PORT);
            // 设置读超时60秒
            socket.setSoTimeout(60000);

            writer = new PrintWriter(socket.getOutputStream(), true);
            reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));


            // 首先读取服务端欢迎消息
            System.out.println("开始读取服务端欢迎消息...");
            String welcome = Base64Util.decode(reader.readLine());
            messageArea.append(welcome + "\n");

            // 然后检查版本
            System.out.println("开始执行版本检查...");
            checkVersion();

            // 启动接收消息线程
            new Thread(new MessageReceiver()).start();

            return true;
        } catch (IOException e) {
            System.err.println("连接过程中出现异常：" + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }


    // 添加执行更新脚本的方法
    private void executeUpdateScript() {
        try {
            String scriptName = System.getProperty("os.name").toLowerCase().contains("win")
                    ? "update.bat" : "update.sh";

            // 延迟执行以确保当前进程可以退出
            new Thread(() -> {
                try {
                    Thread.sleep(1000);
                    Runtime.getRuntime().exec(scriptName);
                    System.exit(0);
                } catch (Exception e) {
                    messageArea.append("执行升级脚本失败: " + e.getMessage() + "\n");
                }
            }).start();

        } catch (Exception e) {
            messageArea.append("准备升级脚本失败: " + e.getMessage() + "\n");
        }
    }
    // 添加接收升级文件方法
    private void receiveUpgradeFile() {
        try {
            String fileName = null;
            StringBuilder fileContent = new StringBuilder();
            long expectedChecksum = 0;

            while (true) {
                String encodedData = reader.readLine();
                if (encodedData == null) break;

                String decodedData = Base64Util.decode(encodedData);

                if (decodedData.startsWith("CHECKSUM:")) {
                    expectedChecksum = Long.parseLong(decodedData.substring(9));
                } else if (decodedData.startsWith("FILE_START:")) {
                    fileName = decodedData.substring(11);
                    fileContent.setLength(0);
                } else if (decodedData.equals("FILE_END")) {
                    if (fileName != null) {
                        // 保存文件为临时文件
                        Base64Util.decodeFile(fileContent.toString(), TEMP_CLIENT_JAR);

                        // Verify the saved file
                        long fileChecksum = CRC32Util.calculateFile(TEMP_CLIENT_JAR);
                        if (fileChecksum != expectedChecksum) {
                            messageArea.append("保存文件校验失败: 校验和不匹配 (预期: " + expectedChecksum +
                                    ", 实际: " + fileChecksum + ")\n");
                            new File(TEMP_CLIENT_JAR).delete();
                            break;
                        }

                        // 创建批处理文件用于替换旧版本
                        createUpdateScript();
                        messageArea.append("新版本已保存为: " + TEMP_CLIENT_JAR + "\n");
                        break;
                    }
                } else if (fileName != null) {
                    fileContent.append(decodedData);
                }
            }
        } catch (Exception e) {
            messageArea.append("下载新版本失败: " + e.getMessage() + "\n");
        }
    }
    // 修改createUpdateScript方法
    private void createUpdateScript() throws IOException {
        String scriptContent;
        if (System.getProperty("os.name").toLowerCase().contains("win")) {
            // Windows批处理脚本
            scriptContent = "@echo off\n" +
                    "timeout /t 3 /nobreak >nul\n" +
                    "del " + CLIENT_JAR + "\n" +
                    "move /Y " + TEMP_CLIENT_JAR + " " + CLIENT_JAR + "\n" +
                    "start " + CLIENT_JAR + "\n" +
                    "del update.bat\n";
            Files.write(Paths.get("update.bat"), scriptContent.getBytes());
        } else {
            // Linux/Mac shell脚本
            scriptContent = "#!/bin/sh\n" +
                    "sleep 3\n" +
                    "rm -f " + CLIENT_JAR + "\n" +
                    "mv " + TEMP_CLIENT_JAR + " " + CLIENT_JAR + "\n" +
                    "java -jar " + CLIENT_JAR + "\n" +
                    "rm update.sh\n";
            Files.write(Paths.get("update.sh"), scriptContent.getBytes());
            // 设置执行权限
            new File("update.sh").setExecutable(true);
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

    public synchronized void sendData(String data) throws IOException {
        if (!socket.isConnected()) {
            throw new IOException("连接未建立或已关闭");
        }
        writer.println(Base64Util.encode(data)); // 按行发送

        // 启动一个新线程来读取服务器响应
        new Thread(() -> {
            try {
                // 读取服务端响应
                String response = Base64Util.decode(reader.readLine());
                messageArea.append("服务器响应: " + response + "\n");
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }).start();
    }

    // 发送普通消息（自动添加MSG协议头）
    public void sendMessage(String message) throws IOException {
        sendData("MSG:" + message);
    }

    // 发送文件
    public void sendFile(String filePath) {
        try {
            File file = new File(filePath);
            String fileName = file.getName();
            long checksum = CRC32Util.calculateFile(filePath);
            // 发送校验和
            writer.println(Base64Util.encode("CHECKSUM:" + checksum));

            String encodedFile = Base64Util.encodeFile(filePath);

            // 分块发送
            int chunkSize = 8192; // 8KB每块
            writer.println(Base64Util.encode("FILE_START:" + fileName));

            for (int i = 0; i < encodedFile.length(); i += chunkSize) {
                int end = Math.min(encodedFile.length(), i + chunkSize);
                writer.println(Base64Util.encode(encodedFile.substring(i, end)));
            }

            writer.println(Base64Util.encode("FILE_END"));
            messageArea.append("文件已发送: " + fileName + " (校验和: " + checksum + ")\n");

        } catch (Exception e) {
            System.err.println("文件传输失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private class MessageReceiver implements Runnable {
        @Override
        public void run() {
            try {
                while (socket.isConnected()) {
                    try {
                        String encodedData = reader.readLine();
                        if (encodedData == null) {
                            // 服务端关闭连接
                            break;
                        }
                        String decodedData = Base64Util.decode(encodedData);
                        messageArea.append("服务器消息: " + decodedData + "\n");
                    } catch (java.net.SocketTimeoutException e) {
                        // 读超时，发送心跳保持连接
                        writer.println(Base64Util.encode("HEARTBEAT"));
                        continue;
                    }
                }
            } catch (IOException e) {
                if (!socket.isClosed()) {
                    messageArea.append("连接异常: " + e.getMessage() + "\n");
                }
            } finally {
                messageArea.append("与服务器的连接已断开\n");
            }
        }
    }

    public static void main(String[] args) {
        // 检查是否有临时升级文件
        boolean hasUpdate = new File(TEMP_CLIENT_JAR).exists();

        if (hasUpdate) {
            try {
                // 替换旧版本
                Files.move(Paths.get(TEMP_CLIENT_JAR), Paths.get(CLIENT_JAR),
                        StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
                System.err.println("更新失败: " + e.getMessage());
            }
        }

        SwingUtilities.invokeLater(() -> {
            ClientGUI_v2 ClientGUI_v2 = new ClientGUI_v2();
            ClientGUI_v2.setVisible(true);
            if (hasUpdate) {
                ClientGUI_v2.messageArea.append("已成功更新到新版本\n");
            }
        });
    }
}
