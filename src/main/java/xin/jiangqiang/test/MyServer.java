package xin.jiangqiang.test;

import cn.hutool.core.thread.ThreadUtil;
import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.CharsetUtil;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public class MyServer {

    public static void main(String[] args) throws Exception {
        ThreadUtil.execute(() -> {
            //创建两个线程组 boosGroup、workerGroup
            EventLoopGroup bossGroup = new NioEventLoopGroup();
            EventLoopGroup workerGroup = new NioEventLoopGroup();
            try {
                //创建服务端的启动对象，设置参数
                ServerBootstrap bootstrap = new ServerBootstrap();
                //设置两个线程组boosGroup和workerGroup
                bootstrap.group(bossGroup, workerGroup)
                        //设置服务端通道实现类型
                        .channel(NioServerSocketChannel.class)
                        //设置线程队列得到连接个数
                        .option(ChannelOption.SO_BACKLOG, 128)
                        //设置保持活动连接状态
                        .childOption(ChannelOption.SO_KEEPALIVE, true)
                        //使用匿名内部类的形式初始化通道对象
                        .childHandler(new ChannelInitializer<SocketChannel>() {
                            @Override
                            protected void initChannel(SocketChannel socketChannel) {
                                //给pipeline管道设置处理器
                                socketChannel.pipeline().addLast(new ServerHandler());
                            }
                        });//给workerGroup的EventLoop对应的管道设置处理器
                System.out.println("java技术爱好者的服务端已经准备就绪...");
                //绑定端口号，启动服务端
                ChannelFuture channelFuture = bootstrap.bind(80).sync();
                //对关闭通道进行监听
                channelFuture.channel().closeFuture().sync();
            } catch (Exception e) {
            } finally {
                bossGroup.shutdownGracefully();
                workerGroup.shutdownGracefully();
            }
        });
        ThreadUtil.execute(() -> {
            NioEventLoopGroup eventExecutors = new NioEventLoopGroup();
            try {
                //创建bootstrap对象，配置参数
                Bootstrap bootstrap = new Bootstrap();
                //设置线程组
                bootstrap.group(eventExecutors)
                        //设置客户端的通道实现类型
                        .channel(NioSocketChannel.class)
                        //使用匿名内部类初始化通道
                        .handler(new ChannelInitializer<SocketChannel>() {
                            @Override
                            protected void initChannel(SocketChannel ch) {
                                //添加客户端通道的处理器
                                ch.pipeline().addLast(new ClientHandler());
                            }
                        });
                System.out.println("客户端准备就绪，随时可以起飞~");
                //连接服务端
                ChannelFuture channelFuture = bootstrap.connect("router.jiangqiang.xin", 8090).sync();
                //对通道关闭进行监听
                channelFuture.channel().closeFuture().sync();
            } catch (Exception e) {
            } finally {
                //关闭线程组
                eventExecutors.shutdownGracefully();
            }
        });
    }

    private static final BlockingQueue<ByteBuf> requestQueue = new ArrayBlockingQueue<>(1024);//数据从其他客户端传到此程序，并转发到最终地址
    private static final BlockingQueue<ByteBuf> responseQueue = new ArrayBlockingQueue<>(1024);//数据从最终地址响应到此服务器，并返回到客户端

    static class ServerHandler extends ChannelInboundHandlerAdapter {

        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
            //获取其他客户端发送过来的消息
            ByteBuf byteBuf = (ByteBuf) msg;
            requestQueue.put(byteBuf);
            System.out.println("收到客户端" + ctx.channel().remoteAddress() + "发送的消息：" + byteBuf.toString(CharsetUtil.UTF_8));
        }

        @Override
        public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
            //发送消息给客户端
            ctx.writeAndFlush(responseQueue.take());
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
            //发生异常，关闭通道
            ctx.close();
        }
    }

    static class ClientHandler extends ChannelInboundHandlerAdapter {

        @Override
        public void channelActive(ChannelHandlerContext ctx) throws Exception {
            //发送消息到服务端
            ctx.writeAndFlush(requestQueue.take());
        }

        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
            //接收服务端发送过来的消息
            ByteBuf byteBuf = (ByteBuf) msg;
            responseQueue.put(byteBuf);
            System.out.println("收到服务端" + ctx.channel().remoteAddress() + "的消息：" + byteBuf.toString(CharsetUtil.UTF_8));
        }
    }

}