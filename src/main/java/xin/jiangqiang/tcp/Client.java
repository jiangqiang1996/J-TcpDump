package xin.jiangqiang.tcp;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.Date;

public class Client {
    public static void main(String[] args) throws IOException {
        System.out.println(new Date());
        Socket client = new Socket("127.0.0.1", 8080);
        client.close();

//        byte[] buffer = new byte[1024];
//        try {
//            while (new DataInputStream(client.getInputStream()).read(buffer) > -1) {
//                System.out.println(new String(buffer));
//            }
//        } catch (IOException e) {
//            System.out.println(e.getMessage());
//            throw new RuntimeException(e);
//        }
        System.out.println(new Date());
    }

}
