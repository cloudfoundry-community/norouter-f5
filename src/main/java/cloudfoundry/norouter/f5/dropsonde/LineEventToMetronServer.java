/*
 * Copyright (c) 2015 Intellectual Reserve, Inc.  All rights reserved.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
package cloudfoundry.norouter.f5.dropsonde;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.LineBasedFrameDecoder;

/**
 * @author Mike Heath
 */
// TODO Create codec that creates object instances based on log line from HSL
// TODO Start with LOG
// TODO Add support for limiting incoming message to certain CIDR ranges
// TODO Move to F5 module
// TODO Can we even support HttpStart, HttpStop, HttpStartStop?
public class LineEventToMetronServer {

	public static void main(String[] args) {
		final EventLoopGroup boss = new NioEventLoopGroup(1);
		final EventLoopGroup worker = new NioEventLoopGroup(1);

		final ServerBootstrap bootstrap = new ServerBootstrap();
		bootstrap
				.group(boss, worker)
				.channel(NioServerSocketChannel.class)
				.childHandler(new ChannelInitializer<SocketChannel>() {
					@Override
					protected void initChannel(SocketChannel ch) throws Exception {
						ch.pipeline().addLast(new LineBasedFrameDecoder(64 * 1024));
						ch.pipeline().addLast(new LineEventDecoder());
						ch.pipeline().addLast(new ChannelInboundHandlerAdapter() {
							@Override
							public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
								// TODO Use a logger to log this exception
								cause.printStackTrace();
								ctx.close();
							}

							@Override
							public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
								System.out.println("** New connection from: " + ctx.channel().remoteAddress());
							}

							@Override
							public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
								if (msg instanceof LogEvent) {
									final LogEvent logEvent = (LogEvent) msg;
									System.out.println(logEvent.getTimestamp() + " " + logEvent.getApplicationAddress() + " --- " + logEvent.getMessage());
								} else {
									super.channelRead(ctx, msg);
								}
							}
						});
					}
				});
		bootstrap.bind(8007).syncUninterruptibly();
	}

}
