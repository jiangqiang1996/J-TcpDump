package xin.jiangqiang.tcp;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

@Accessors(chain = true)
@Getter
@Slf4j
public class ServiceA {
    private final Integer port;//此程序监听端口，转发来自此端口的数据
    private final String remoteHost;//转发的目标地址
    private final Integer remotePort;//目的端口
    private final ServerSocket serverSocket;//监听来的请求
    private List<TaskInstance> taskInstanceList = new ArrayList<>();

    @Getter
    private final class TaskInstance {
        private Socket server;//数据来源与此程序之间的通信
        private Socket client;//此程序与数据最终地址的通信
        @Setter
        private boolean serverIsEnableClosed = false;
        @Setter
        private boolean clientIsEnableClosed = false;

        public TaskInstance() throws IOException {
            server = serverSocket.accept();
            client = new Socket(remoteHost, remotePort);
        }

        public void close() {
            ServiceA.close(server, client);
        }

        /**
         * 根据条件判断，尝试关闭
         */
        public boolean autoClose() {
            if (serverIsEnableClosed && clientIsEnableClosed) {
                close();
                return true;
            }
            return false;
        }
    }

    public TaskInstance getTaskInstance() throws IOException {
        return new TaskInstance();
    }

    //todo service应该是单例
    public ServiceA(Integer port, String remoteHost, Integer remotePort) throws IOException {
        this.port = port;
        this.remoteHost = remoteHost;
        this.remotePort = remotePort;
        this.serverSocket = new ServerSocket(port);
    }

    /**
     * socket1的数据转发到socket2
     *
     * @param socket1
     * @param socket2
     */
    private static void write(Socket socket1, Socket socket2) {
        try {
            write(socket1.getInputStream(), socket2.getOutputStream());
        } catch (IOException e) {
            System.out.println(e.getMessage());
            throw new RuntimeException(e);
        }
    }

    private static void write(InputStream inputStream, OutputStream outputStream) {
        int length;
        byte[] buffer = new byte[1024];
        try {
            while ((length = new DataInputStream(inputStream).read(buffer)) > -1) {
                log.info(new String(buffer));
                new DataOutputStream(outputStream).write(buffer, 0, length);
            }
        } catch (IOException e) {
            System.out.println(e.getMessage());
            throw new RuntimeException(e);
        }
    }

    public static void main(String[] args) throws IOException {
//        Service service = new Service(80, "127.0.0.1", 8848);
        ServiceA service = new ServiceA(80, "192.168.1.2", 22);
        while (true) {
            TaskInstance taskInstance = service.getTaskInstance();
            service.getTaskInstanceList().add(taskInstance);
            new Thread(() -> {
                new Thread(() -> {
                    write(taskInstance.getServer(), taskInstance.getClient());
                    log.info("请求数据转发完毕");
//                    taskInstance.setClientIsEnableClosed(true);
//                    boolean flag = taskInstance.autoClose();
//                    if (flag) {
//                        service.getTaskInstanceList().remove(taskInstance);
//                        log.info("请求数据转发完毕，并关闭资源完毕");
//                    }
                }).start();
                new Thread(() -> {
                    write(taskInstance.getClient(), taskInstance.getServer());
                    log.info("响应数据转发完毕");
//                    taskInstance.setServerIsEnableClosed(true);
//                    boolean flag = taskInstance.autoClose();
//                    if (flag) {
//                        service.getTaskInstanceList().remove(taskInstance);
//                        log.info("响应数据转发完毕，并关闭资源完毕");
//                    }
                }).start();
            }).start();
        }
    }

    public static void close(Socket... sockets) {
        for (Socket socket : sockets) {
            if (socket != null && !socket.isClosed()) {
                try {
                    log.info("socket关闭");
                    socket.close();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }
}
