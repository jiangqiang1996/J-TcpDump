package xin.jiangqiang.sample;

import xin.jiangqiang.tcp.Service;

/**
 * @Author: JiangQiang
 * @Date: 2022年05月29日 13:24
 */
public class Test {
    public static void main(String[] args) {
        Service service1 = new Service(80, "router.jiangqiang.xin", 8090, 3);
        Service service2 = new Service(22, "192.168.1.2", 22, 60);
        service1.start();
        service2.start();
    }
}
