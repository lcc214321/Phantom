package com.phantom.dispatcher.server;

import com.phantom.dispatcher.config.DispatcherConfig;
import com.phantom.dispatcher.zookeeper.ZookeeperManager;
import com.phantom.common.Constants;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.DelimiterBasedFrameDecoder;
import lombok.extern.slf4j.Slf4j;

/**
 * 分发系统服务器
 *
 * @author Jianfeng Wang
 * @since 2019/11/8 16:41
 */
@Slf4j
public class DispatcherServer {

    private DispatcherConfig config;

    public DispatcherServer(DispatcherConfig config) {
        this.config = config;
    }

    public void initialize() {
        EventLoopGroup connectionThreadGroup = new NioEventLoopGroup();
        EventLoopGroup ioThreadGroup = new NioEventLoopGroup();
        try {
            ZookeeperManager zookeeperManager = new ZookeeperManager(config);
            ServerBootstrap server = new ServerBootstrap();
            server.group(connectionThreadGroup, ioThreadGroup)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new ChannelInitializer<SocketChannel>() {

                        @Override
                        protected void initChannel(SocketChannel socketChannel) throws Exception {
                            ByteBuf delimiter = Unpooled.copiedBuffer(Constants.DELIMITER);
                            socketChannel.pipeline().addLast(new DelimiterBasedFrameDecoder(config.getMaxMessageBytes(), delimiter));
                            socketChannel.pipeline().addLast(new DispatcherHandler());
                        }

                    });
            ChannelFuture channelFuture = server.bind(config.getPort()).sync();
            log.info("分发系统已经启动......监听端口：{}", config.getPort());
            channelFuture.sync();
            channelFuture.addListener((ChannelFutureListener) channelFuture1 -> zookeeperManager.createNode());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


}
