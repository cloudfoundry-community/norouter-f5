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

import java.net.InetSocketAddress;
import java.time.Instant;
import java.util.UUID;

/**
 * @author Mike Heath
 */
class LogEvent implements LineEvent {
	private final String ltmIdentifier;
	private final InetSocketAddress applicationAddress;
	private final Instant timestamp;
	private final UUID requestId;
	private final String message;

	public LogEvent(String ltmIdentifier, InetSocketAddress applicationAddress, Instant timestamp, UUID requestId, String message) {
		this.ltmIdentifier = ltmIdentifier;
		this.applicationAddress = applicationAddress;
		this.timestamp = timestamp;
		this.requestId = requestId;
		this.message = message;
	}

	public String getLtmIdentifier() {
		return ltmIdentifier;
	}

	public InetSocketAddress getApplicationAddress() {
		return applicationAddress;
	}

	public Instant getTimestamp() {
		return timestamp;
	}

	public String getMessage() {
		return message;
	}

	public UUID getRequestId() {
		return requestId;
	}


}
