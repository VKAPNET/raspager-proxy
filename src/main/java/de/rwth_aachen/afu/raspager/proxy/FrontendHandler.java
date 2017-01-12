/*
 * Copyright (C) 2016 Amateurfunkgruppe der RWTH Aachen
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package de.rwth_aachen.afu.raspager.proxy;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelOption;
import java.net.SocketAddress;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The frontend handler is responsible for the connection to the frontend
 * server. It will open a connection to the backend server once the connection
 * to the frontend server has been established.
 *
 * @author Philipp Thiel
 */
class FrontendHandler extends ChannelInboundHandlerAdapter {

    private static final Logger logger = Logger.getLogger(FrontendHandler.class.getName());
    private final SocketAddress backendAddress;
    private Channel outboundChannel;

    /**
     * Creates a new frontend handler with the given backend server address.
     *
     * @param backendAddress Backend server address and port to use.
     */
    public FrontendHandler(SocketAddress backendAddress) {
        this.backendAddress = backendAddress;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        logger.info("Connected to frontend server.");

        final Channel inboundChannel = ctx.channel();

        Bootstrap b = new Bootstrap();
        b.group(inboundChannel.eventLoop());
        b.channel(ctx.channel().getClass());
        b.handler(new BackendHandler(inboundChannel));
        b.option(ChannelOption.AUTO_READ, false);

        ChannelFuture f = b.connect(backendAddress);
        outboundChannel = f.channel();
        f.addListener((ChannelFuture ff) -> {
            if (ff.isSuccess()) {
                inboundChannel.read();
            } else {
                inboundChannel.close();
            }
        });
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (outboundChannel.isActive()) {
            outboundChannel.writeAndFlush(msg).addListener((ChannelFuture f) -> {
                if (f.isSuccess()) {
                    ctx.channel().read();
                } else {
                    f.channel().close();
                }
            });
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        logger.info("Disconnected from frontend handler.");

        if (outboundChannel != null) {
            closeOnFlush(outboundChannel);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        logger.log(Level.SEVERE, "Exception in frontend handler.", cause);
        closeOnFlush(ctx.channel());
    }

    static void closeOnFlush(Channel ch) {
        if (ch.isActive()) {
            ch.writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE);
        }
    }
}