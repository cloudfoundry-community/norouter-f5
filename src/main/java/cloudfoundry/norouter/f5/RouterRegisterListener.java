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

/**
 * @author Mike Heath
 */
// TODO Spring Boot app in core
// TODO Create F5 "plugin"
// TODO Router unregister listener
// TODO Come up with description JSON
// TODO Come up with something to reconcile differences in RouterTable and LTM
// TODO Add something to populate RouteTable with contents of F5 to give Route table an 'initial state' before starting.
// TODO Make a sweet README file with architecture diagrams and stuff
// TODO Create BOSH release
public class RouterRegisterListener implements ApplicationListener<RouteRegisterEvent> {

	private static final Logger LOGGER = LoggerFactory.getLogger(RouterRegisterListener.class);

	private final String poolNamePrefix;
	private final IControlClient client;

	public RouterRegisterListener(String poolNamePrefix, IControlClient client) {
		this.poolNamePrefix = poolNamePrefix;
		this.client = client;
	}

	@Override
	public void onApplicationEvent(RouteRegisterEvent event) {
		final String poolName = poolNamePrefix + event.getHost();

		final Pool pool = Pool.create()
				.name(poolName)
				.description("We need a good JSON description with some meta information")
				.build();

		try {
			client.createPool(pool);
			LOGGER.info("Created pool {}", poolName);
		} catch (ConflictException e) {
			// Pool already exists, good
		}

		try {
			client.addPoolMember(poolName, event.getAddress(), "We need a good JSON description here");
			LOGGER.info("Added pool member {} to pool {}", event.getAddress(), poolName);
		} catch (ConflictException e) {
			// Pool member already exists
		}
	}
}
