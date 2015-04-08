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

import cf.dropsonde.MetronClient;
import cloudfoundry.norouter.routingtable.RouteDetails;
import cloudfoundry.norouter.routingtable.RouteRegistrar;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.LineBasedFrameDecoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Mike Heath
 */
// TODO Add support for limiting incoming message to certain CIDR ranges
// TODO Support HttpStart, HttpStop, HttpStartStop events
// TODO Make port configurable
// TODO Auto register this norouter with LTM pool used for logging
public class LineEventToMetronServer {

	private static final Logger LOGGER = LoggerFactory.getLogger(LineEventToMetronServer.class);

	public LineEventToMetronServer(EventLoopGroup boss, EventLoopGroup worker, RouteRegistrar routeRegistrar, MetronClient metronClient) {
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
								LOGGER.warn("An error occurred processing logging events from the LTM.", cause);
								ctx.close();
							}

							@Override
							public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
								LOGGER.info("New connection from {}", ctx.channel().remoteAddress());
							}

							@Override
							public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
								if (msg instanceof LogEvent) {
									final LogEvent logEvent = (LogEvent) msg;
									final RouteDetails routeDetails = routeRegistrar.getRouteByAddress(logEvent.getApplicationAddress());
									if (routeDetails != null && routeDetails.getApplicationGuid() != null) {
										final String appGuid = routeDetails.getApplicationGuid().toString();
										final String message = logEvent.getMessage() + " app_id:" + appGuid;
										metronClient.createLogEmitter("RTR", logEvent.getLtmIdentifier()).emit(logEvent.getTimestamp(), appGuid, message);
									}
								} else {
									super.channelRead(ctx, msg);
								}
							}
						});
					}
				});
		final int port = 8007;
		bootstrap.bind(port).syncUninterruptibly();
		LOGGER.info("Listening for logging events from the LTM on port {}", port);
	}

}
