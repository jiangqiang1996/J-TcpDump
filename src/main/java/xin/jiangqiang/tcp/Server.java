package xin.jiangqiang.tcp;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Date;

public class Server {
    public static void main(String[] args) throws IOException, InterruptedException {
        ServerSocket serverSocket = new ServerSocket(8080);
//        while (true) {
        Socket server = serverSocket.accept();
        new Thread(() -> {
            while (true) {
                try {
                    System.out.println(server.isClosed());
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }

            }
        }).start();
        System.out.println(new Date());
        Thread.sleep(10000);
        server.close();
        System.out.println(new Date());

        Thread.sleep(10000);
        System.out.println(new Date());
//        }

    }

}
