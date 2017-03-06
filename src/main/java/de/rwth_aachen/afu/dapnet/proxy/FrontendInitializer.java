/*
 * Copyright (C) 2017 Amateurfunkgruppe der RWTH Aachen
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
package de.rwth_aachen.afu.dapnet.proxy;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.DelimiterBasedFrameDecoder;
import io.netty.handler.codec.Delimiters;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;

/**
 * This class initializes the frontend channel pipeline.
 *
 * @author Philipp Thiel
 */
class FrontendInitializer extends ChannelInitializer<SocketChannel> {

    // TODO Use ASCII charset instead?
    private static final StringDecoder DECODER = new StringDecoder();
    private static final StringEncoder ENCODER = new StringEncoder();
    private static final LineBreakAdder LBA = new LineBreakAdder();
    private final WelcomeMessageEncoder msgEncoder;
    private final Settings settings;

    public FrontendInitializer(Settings settings) {
        this.msgEncoder = new WelcomeMessageEncoder(settings.getFrontendName(),
                settings.getFrontendKey());
        this.settings = settings;
    }

    @Override
    protected void initChannel(SocketChannel ch) throws Exception {
        ChannelPipeline p = ch.pipeline();
        p.addLast(new DelimiterBasedFrameDecoder(1024, Delimiters.lineDelimiter()));
        p.addLast(DECODER);
        p.addLast(ENCODER);
        p.addLast(LBA);
        p.addLast(msgEncoder);
        p.addLast(new FrontendHandler(settings));
    }

}
