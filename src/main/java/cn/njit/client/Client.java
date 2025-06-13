package cn.njit.client;

import java.io.IOException;
import java.net.Socket;

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

    // 后续添加数据发送和接收方法

    public static void main(String[] args) {
        Client client = new Client();
        if (client.connect()) {
            // 连接成功后执行操作
            client.disconnect();
        }
    }
}
