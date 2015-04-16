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

import cloudfoundry.norouter.NorouterUtil;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.DecoderException;
import io.netty.handler.codec.MessageToMessageDecoder;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * @author Mike Heath
 */
public class LineEventDecoder extends MessageToMessageDecoder<ByteBuf> {

	@Override
	protected void decode(ChannelHandlerContext context, ByteBuf buffer, List<Object> out) throws Exception {
		buffer.markReaderIndex();
		try {
			final String command = nextString(buffer);
			// Extract event type
			switch (command) {
				case "LOG":
					decodeLogEvent(context, buffer, out);
					break;
				default:
					throw new IllegalStateException("Unknown command " + command);
			}
		} catch (Exception e) {
			buffer.resetReaderIndex();
			throw new DecoderException("Invalid line event: " + buffer.toString(StandardCharsets.UTF_8), e);
		}
	}

	private String nextString(ByteBuf buffer) {
		// Read space terminated string
		final int start = buffer.readerIndex();
		int length = 0;
		while (buffer.readByte() != ' ') {
			length++;
		}
		return buffer.toString(start, length, StandardCharsets.UTF_8);
	}

	private void decodeLogEvent(ChannelHandlerContext context, ByteBuf buffer, List<Object> out) {
		final String ltmIdentifier = nextString(buffer);
		final String address = nextString(buffer);
		final String timestamp = nextString(buffer);
		final String requestId = nextString(buffer);
		final String message = buffer.toString(StandardCharsets.UTF_8);
		out.add(new LogEvent(
				ltmIdentifier,
				NorouterUtil.toSocketAddress(address),
				Instant.ofEpochMilli(Long.valueOf(timestamp)),
				UUID.fromString(requestId),
				message
		));
	}

}
