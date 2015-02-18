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

import cloudfoundry.norouter.f5.client.ConflictException;
import cloudfoundry.norouter.f5.client.IControlClient;
import cloudfoundry.norouter.f5.client.Pool;
import cloudfoundry.norouter.routingtable.RouteRegisterEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationListener;
import org.springframework.core.Ordered;

import java.time.Instant;

/**
 * @author Mike Heath
 */
// TODO Come up with something to reconcile differences in RouterTable and LTM
// TODO Add something to populate RouteTable with contents of F5 to give Route table an 'initial state' before starting.
// TODO Make a sweet README file with architecture diagrams and stuff
// TODO Create BOSH release
public class RouteRegisterListener implements ApplicationListener<RouteRegisterEvent>, Ordered {

	private static final Logger LOGGER = LoggerFactory.getLogger(RouteRegisterListener.class);

	private final String poolNamePrefix;
	private final IControlClient client;

	public RouteRegisterListener(String poolNamePrefix, IControlClient client) {
		this.poolNamePrefix = poolNamePrefix;
		this.client = client;
	}

	@Override
	public void onApplicationEvent(RouteRegisterEvent event) {
		final String poolName = poolNamePrefix + event.getHost();

		final PoolDescription poolDescription = new PoolDescription(Instant.now(), Instant.now());
		final String poolDescriptionJsonish = poolDescription.toJsonish();

		final Pool pool = Pool.create()
				.name(poolName)
				.description(poolDescriptionJsonish)
				.build();

		boolean poolCreated = false;
		try {
			client.createPool(pool);
			LOGGER.info("Created pool {}", poolName);
			poolCreated = true;
		} catch (ConflictException e) {
			// Pool already exists, good
		}

		try {
			client.addPoolMember(poolName, event.getAddress(), "We need a good JSON description here");
			LOGGER.info("Added pool member {} to pool {}", event.getAddress(), poolName);

			if (!poolCreated) {
				// If we didn't create the pool but added the pool member, update the modified fields in the
				// pool description
				Util.updatePoolModifiedTimestamp(client, poolName);
				LOGGER.debug("Updated modified field on pool {}", poolName);
			}
		} catch (ConflictException e) {
			// Pool member already exists
		}
	}

	@Override
	public int getOrder() {
		return 0;
	}
}
