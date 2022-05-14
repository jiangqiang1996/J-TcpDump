package xin.jiangqiang.tcp;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.Synchronized;
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
public class Service {
    private static Service service;
    private final Integer port;//此程序监听端口，转发来自此端口的数据
    private final String remoteHost;//转发的目标地址
    private final Integer remotePort;//目的端口
    private final Integer soTimeOut;//超时时间，单位秒
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

        public TaskInstance() throws Exception {
            try {
                server = serverSocket.accept();
                client = new Socket(remoteHost, remotePort);
                server.setSoTimeout(soTimeOut * 1000);
                client.setSoTimeout(soTimeOut * 1000);
            } catch (Exception e) {
                close();
                log.info("出现异常强制关闭");
                throw new Exception(e);
            }
        }

        /**
         * 关闭server和client
         */
        public void close() {
            Service.close(server, client);
        }

        /**
         * 根据条件判断，尝试关闭
         */
        public boolean autoClose() {
            if (serverIsEnableClosed && clientIsEnableClosed) {
                close();
                log.info("根据条件关闭成功");
                return true;
            }
            return false;
        }
    }

    public TaskInstance getTaskInstance() {
        try {
            return new TaskInstance();
        } catch (Exception e) {
            log.debug("任务实例创建失败" + e.getMessage());
            return null;
        }
    }

    /***
     *
     * @param port
     * @param remoteHost
     * @param remotePort
     * @param soTimeOut minutes TimeOut
     * @throws IOException
     */
    private Service(Integer port, String remoteHost, Integer remotePort, Integer soTimeOut) throws IOException {
        this.port = port;
        this.remoteHost = remoteHost;
        this.remotePort = remotePort;
        this.serverSocket = new ServerSocket(port);
        this.soTimeOut = soTimeOut;
    }

    public static Service getService(Integer port, String remoteHost, Integer remotePort, Integer soTimeOut) throws IOException {
        if (service == null) {
            synchronized (Service.class) {
                if (service == null) {
                    service = new Service(port, remoteHost, remotePort, soTimeOut);
                    return service;
                }
            }
        }
        return service;
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
            throw new RuntimeException(e);
        }
    }

    private static void write(InputStream inputStream, OutputStream outputStream) {
        int length;
        byte[] buffer = new byte[1024];
        try {
            while ((length = new DataInputStream(inputStream).read(buffer)) > -1) {
                log.info("\n"+new String(buffer));//转发的数据内容
                new DataOutputStream(outputStream).write(buffer, 0, length);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void main(String[] args) throws IOException {
        Service service = getService(80, "127.0.0.1", 8848, 3);//转发http通信可以把时间设置较短
//        Service service = getService(80, "192.168.1.2", 22, 60);//连接ssh后，超过时间没有通信就会被关闭
        while (true) {
            TaskInstance taskInstance = service.getTaskInstance();
            if (taskInstance == null) {
                continue;
            }
            service.getTaskInstanceList().add(taskInstance);
            new Thread(() -> {
                try {
                    write(taskInstance.getServer(), taskInstance.getClient());
                    log.info(taskInstance + "请求数据转发完毕");
                } catch (Exception e) {
                    log.debug("请求数据转发过程出错：" + taskInstance + e.getMessage());
                } finally {
                    taskInstance.setClientIsEnableClosed(true);
                    boolean flag = taskInstance.autoClose();
                    if (flag) {
                        service.getTaskInstanceList().remove(taskInstance);
                        log.info(taskInstance + "关闭资源完毕");
                    }
                }
            }).start();
            new Thread(() -> {
                try {
                    write(taskInstance.getClient(), taskInstance.getServer());
                    log.info(taskInstance + "响应数据转发完毕");
                } catch (Exception e) {
                    log.debug("响应数据转发过程出错：" + taskInstance + e.getMessage());
                } finally {
                    taskInstance.setServerIsEnableClosed(true);
                    boolean flag = taskInstance.autoClose();
                    if (flag) {
                        service.getTaskInstanceList().remove(taskInstance);
                        log.info(taskInstance + "关闭资源完毕");
                    }
                }
            }).start();
            new Thread(() -> {
                while (true) {
                    List<TaskInstance> taskInstanceList1 = service.getTaskInstanceList();
                    try {
                        Thread.sleep(5000);
                        log.debug("task数量" + taskInstanceList1.size());
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
            }).start();
        }
    }

    public static void close(Socket... sockets) {
        for (Socket socket : sockets) {
            if (socket != null && !socket.isClosed()) {
                try {
                    log.info(socket + "socket关闭");
                    socket.close();
                } catch (IOException e) {
                    log.debug(socket + "socket关闭失败");
                }
            }
        }
    }
}
