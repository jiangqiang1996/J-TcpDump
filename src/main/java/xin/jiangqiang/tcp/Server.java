package xin.jiangqiang.tcp;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;

public class Server {

    public static void main(String[] args) throws IOException {
        ServerSocket serverSocket = new ServerSocket(80);
        while (true) {
//            new Thread(() -> {
            try {
                Socket server = serverSocket.accept();//作为服务器，连接发过来的请求套接字
                Socket client = new Socket("127.0.0.1", 8848);//连接目标服务器
                new Thread(() -> {
                    int length;
                    byte[] buffer = new byte[1024];
                    try {
                        while ((length = new DataInputStream(server.getInputStream()).read(buffer)) > -1) {
                            client.getOutputStream().write(buffer, 0, length);
                        }
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }).start();

                new Thread(() -> {
                    int length;
                    byte[] buffer = new byte[1024];
                    try {
                        while ((length = new DataInputStream(client.getInputStream()).read(buffer)) > -1) {
                            server.getOutputStream().write(buffer, 0, length);
                        }
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }).start();

//                client.close();
//                server.close();
            } catch (IOException e) {
                System.err.println(e.getMessage());
            }
//            }).start();
        }
    }
}
