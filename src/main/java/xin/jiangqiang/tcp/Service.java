package xin.jiangqiang.tcp;

import cn.hutool.core.thread.ThreadUtil;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;

@Accessors(chain = true)
@Getter
@Slf4j
public class Service {
    private final Integer port;//此程序监听端口，转发来自此端口的数据
    private final String remoteHost;//转发的目标地址
    private final Integer remotePort;//目的端口
    private final Integer soTimeOut;//超时时间，单位秒
    private final ServerSocket serverSocket;//监听来的请求
    private final ExecutorService executor = ThreadUtil.newExecutor();//将任务直接提交给线程而不保持它们。当运行线程小于maxPoolSize时会创建新线程，否则触发异常策略
    private final List<TaskInstance> taskInstanceList = Collections.synchronizedList(new ArrayList<>());

    @Getter
    private final class TaskInstance {
        private final Socket server;//数据来源与此程序之间的通信
        private final Socket client;//此程序与数据最终地址的通信
        @Setter
        private boolean serverIsEnableClosed = false;
        @Setter
        private boolean clientIsEnableClosed = false;

        private TaskInstance() throws Exception {
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
                return true;
            }
            return false;
        }
    }

    /**
     * 获取一个任务实例
     *
     * @return
     */
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
     * @param port 服务端口号
     * @param remoteHost 目的地址
     * @param remotePort 目的端口号
     * @param soTimeOut 超时时间，单位分钟
     * @throws IOException
     */
    public Service(Integer port, String remoteHost, Integer remotePort, Integer soTimeOut) {
        try {
            this.port = port;
            this.remoteHost = remoteHost;
            this.remotePort = remotePort;
            this.serverSocket = new ServerSocket(port);
            this.soTimeOut = soTimeOut;
        } catch (IOException e) {
            log.info("Service初始化失败");
            throw new RuntimeException(e);
        }
    }


    /**
     * socket1的数据转发到socket2
     *
     * @param socket1
     * @param socket2
     */
    private static void write(Socket socket1, Socket socket2) {
        try {
            int length;
            byte[] buffer = new byte[1024];
            while ((length = new DataInputStream(socket1.getInputStream()).read(buffer)) > -1) {
                log.debug("\n" + new String(buffer));//转发的数据内容
                new DataOutputStream(socket2.getOutputStream()).write(buffer, 0, length);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


    public void start() {
        log.info("启动成功：[{},{},{},{}]", this.port, this.remoteHost, this.remotePort, this.soTimeOut);
        executor.execute(() -> {
            while (true) {
                TaskInstance taskInstance = getTaskInstance();
                if (taskInstance == null) {
                    continue;
                }
                log.info("获取到实例：{}", taskInstance);
                getTaskInstanceList().add(taskInstance);
                executor.execute(() -> {
                    try {
                        write(taskInstance.getServer(), taskInstance.getClient());
                        log.info(taskInstance + " 请求数据转发完毕");
                    } catch (Exception e) {
                        log.debug("请求数据转发过程出错：" + taskInstance + e.getMessage());
                    } finally {
                        taskInstance.setClientIsEnableClosed(true);
                        boolean flag = taskInstance.autoClose();
                        if (flag) {
                            getTaskInstanceList().remove(taskInstance);
                            log.info(taskInstance + " 关闭资源完毕");
                        }
                    }
                });
                executor.execute(() -> {
                    try {
                        write(taskInstance.getClient(), taskInstance.getServer());
                        log.info(taskInstance + " 响应数据转发完毕");
                    } catch (Exception e) {
                        log.debug("响应数据转发过程出错：" + taskInstance + e.getMessage());
                    } finally {
                        taskInstance.setServerIsEnableClosed(true);
                        boolean flag = taskInstance.autoClose();
                        if (flag) {
                            getTaskInstanceList().remove(taskInstance);
                            log.info(taskInstance + " 关闭资源完毕");
                        }
                    }
                });
                executor.execute(() -> {
                    while (true) {
                        List<TaskInstance> taskInstanceList1 = getTaskInstanceList();
                        try {
                            Thread.sleep(10000);
                            log.info("task数量： " + taskInstanceList1.size());
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                    }
                });
            }
        });
    }

    public static void close(Socket... sockets) {
        for (Socket socket : sockets) {
            if (socket != null && !socket.isClosed()) {
                try {
                    log.info(socket + " 关闭");
                    socket.close();
                } catch (IOException e) {
                    log.debug(socket + " 关闭失败");
                }
            }
        }
    }
}
