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

package cloudfoundry.norouter.f5;

import cloudfoundry.norouter.f5.client.IControlClient;
import cloudfoundry.norouter.f5.client.Pool;
import cloudfoundry.norouter.routingtable.RouteRegistrar;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextStartedEvent;
import org.springframework.core.Ordered;

import java.net.InetSocketAddress;
import java.util.Collection;

/**
 * @author Mike Heath
 */
public class ContextStartedListener implements ApplicationListener<ContextStartedEvent>, Ordered {

	private static final Logger LOGGER = LoggerFactory.getLogger(ContextStartedListener.class);

	private final String poolNamePrefix;
	private final IControlClient client;
	private final RouteRegistrar routeRegistrar;

	public ContextStartedListener(String poolNamePrefix, IControlClient client, RouteRegistrar routeRegistrar) {
		this.poolNamePrefix = poolNamePrefix;
		this.client = client;
		this.routeRegistrar = routeRegistrar;
	}

	@Override
	public void onApplicationEvent(ContextStartedEvent event) {
		client.getAllPools(true).stream()
			.filter(pool -> pool.getName().startsWith(poolNamePrefix))
			.forEach(pool -> pool.getMembers().get().forEach(member -> {
				final String host = pool.getName().substring(poolNamePrefix.length());
				final String[] addressParts = member.getName().split(":");
				final InetSocketAddress address = InetSocketAddress.createUnresolved(addressParts[0], Integer.valueOf(addressParts[1]));
				// TODO Get application fields from description
				LOGGER.info("Registering existing route from F5 for host {} with target {}", host, address);
				routeRegistrar.insertRoute(host, address, null, null, null);
			}));
	}

	@Override
	public int getOrder() {
		return -100;
	}

}
