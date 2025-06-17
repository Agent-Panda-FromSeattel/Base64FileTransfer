package cn.njit.client;

import cn.njit.base64.Base64Util;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.net.Socket;

public class ClientGUI extends JFrame {
    private static final String SERVER_HOST = "localhost";
    private static final int SERVER_PORT = 8888;
    private Socket socket;
    private PrintWriter writer;
    private BufferedReader reader;

    private JTextArea messageArea;
    private JTextField inputField;
    private JButton sendButton;
    private JButton fileButton;

    public ClientGUI() {
        setTitle("客户端");
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
                int result = fileChooser.showOpenDialog(ClientGUI.this);
                if (result == JFileChooser.APPROVE_OPTION) {
                    File selectedFile = fileChooser.getSelectedFile();
                    sendFile(selectedFile.getAbsolutePath());
                }
            }
        });

        connect();
    }

    public boolean connect() {
        try {
            socket = new Socket(SERVER_HOST, SERVER_PORT);
            writer = new PrintWriter(socket.getOutputStream(), true);
            reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            System.out.println("客户端连接到服务端：" + SERVER_HOST + ":" + SERVER_PORT);

            // 读取服务端欢迎消息
            String welcome = Base64Util.decode(reader.readLine());
            messageArea.append(welcome + "\n");

            // 启动接收消息线程
            new Thread(new MessageReceiver()).start();

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
            String encodedFile = Base64Util.encodeFile(filePath);

            // 分块发送
            int chunkSize = 8192; // 8KB每块
            writer.println(Base64Util.encode("FILE_START:" + fileName));

            for (int i = 0; i < encodedFile.length(); i += chunkSize) {
                int end = Math.min(encodedFile.length(), i + chunkSize);
                writer.println(Base64Util.encode(encodedFile.substring(i, end)));
            }

            writer.println(Base64Util.encode("FILE_END"));
            messageArea.append("文件已发送: " + fileName + "\n");

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
                    String encodedData = reader.readLine();
                    if (encodedData != null) {
                        String decodedData = Base64Util.decode(encodedData);
                        messageArea.append("服务器消息: " + decodedData + "\n");
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                ClientGUI clientGUI = new ClientGUI();
                clientGUI.setVisible(true);
            }
        });
    }
}
