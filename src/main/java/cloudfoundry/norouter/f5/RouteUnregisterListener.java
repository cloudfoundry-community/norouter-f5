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
import cloudfoundry.norouter.f5.client.ResourceNotFoundException;
import cloudfoundry.norouter.routingtable.RouteUnregisterEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationListener;
import org.springframework.core.Ordered;

/**
 * @author Mike Heath
 */
public class RouteUnregisterListener implements ApplicationListener<RouteUnregisterEvent>, Ordered {

	private static final Logger LOGGER = LoggerFactory.getLogger(RouteUnregisterListener.class);

	private final String poolNamePrefix;
	private final IControlClient client;

	public RouteUnregisterListener(String poolNamePrefix, IControlClient client) {
		this.poolNamePrefix = poolNamePrefix;
		this.client = client;
	}

	@Override
	public void onApplicationEvent(RouteUnregisterEvent event) {
		final String poolName = poolNamePrefix + event.getHost();
		try {
			LOGGER.info("Removing pool member {} from pool {}", event.getAddress(), poolName);
			client.deletePoolMember(poolName, event.getAddress());
			Util.updatePoolModifiedTimestamp(client, poolName);
			LOGGER.debug("Updated modified field on pool {}", poolName);
		} catch (ResourceNotFoundException e) {
			// Member was already removed
		}
	}

	@Override
	public int getOrder() {
		return 0;
	}

}
