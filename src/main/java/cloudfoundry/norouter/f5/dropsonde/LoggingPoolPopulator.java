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

import cloudfoundry.norouter.NorouterProperties;
import cloudfoundry.norouter.config.F5Properties;
import cloudfoundry.norouter.config.RoutingTableConfig;
import cloudfoundry.norouter.f5.client.IControlClient;
import cloudfoundry.norouter.f5.client.Monitors;
import cloudfoundry.norouter.f5.client.Pool;
import cloudfoundry.norouter.f5.client.PoolMember;
import cloudfoundry.norouter.f5.client.ResourceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.net.InetSocketAddress;
import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;

/**
 * @author Mike Heath
 */
@Component
public class LoggingPoolPopulator {

	private static final Logger LOGGER = LoggerFactory.getLogger(LoggingPoolPopulator.class);

	@Autowired
	NorouterProperties norouterProperties;

	@Autowired
	F5Properties f5Properties;

	@Autowired
	IControlClient client;

	@Scheduled(initialDelay = 0l, fixedRate = 30 * 1000) // Check the logging pool every 30 seconds
	public void registerLoggingPool() {
		updateLoggingPool();
	}

	public void updateLoggingPool() {
		synchronized (this) {
			final InetSocketAddress poolMember = InetSocketAddress.createUnresolved(norouterProperties.getHostAddress(), f5Properties.getLoggingPort());
			final String loggingPoolName = f5Properties.getLoggingPoolName();
			try {
				final Pool loggingPool = client.getPool(loggingPoolName);
				if (!containsSelf(loggingPool.getMembers(), poolMember)) {
					LOGGER.info("Added member {} to pool {}", poolMember, loggingPoolName);
					client.addPoolMember(loggingPoolName, poolMember);
				}
			} catch (ResourceNotFoundException e) {
				LOGGER.info("Creating pool {} with initial member {}", loggingPoolName, poolMember);
				final Pool pool = Pool.create()
						.name(loggingPoolName)
						.monitor(Monitors.TCP_HALF_OPEN)
						.addMember(poolMember)
						.description("Pool used for norouter HSL to forward Dropsonde events to Loggregator.")
						.build();
				client.createPool(pool);
			}
		}
	}

	private boolean containsSelf(Optional<Collection<PoolMember>> poolMembers, InetSocketAddress poolMember) {
		return poolMembers.isPresent()
					&& poolMembers.get().stream()
					.filter(member -> member.getName().equalsIgnoreCase(poolMember.toString()))
					.findFirst().isPresent();
	}

}
