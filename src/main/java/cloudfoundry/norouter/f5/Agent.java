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
import cloudfoundry.norouter.f5.client.Monitors;
import cloudfoundry.norouter.f5.client.Pool;
import cloudfoundry.norouter.f5.client.ResourceNotFoundException;
import cloudfoundry.norouter.routingtable.RouteDetails;
import cloudfoundry.norouter.routingtable.RouteRegistrar;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.time.Instant;
import java.util.Optional;

/**
 * @author Mike Heath
 */
public class Agent {

	private static final Logger LOGGER = LoggerFactory.getLogger(Agent.class);

	private final String poolNamePrefix;
	private final IControlClient client;
	private final RouteRegistrar routeRegistrar;

	public Agent(String poolNamePrefix, IControlClient client, RouteRegistrar routeRegistrar) {
		this.poolNamePrefix = poolNamePrefix;
		this.client = client;
		this.routeRegistrar = routeRegistrar;
	}

	public void populateRouteRegistrar() {
		client.getAllPools(true).stream()
				.filter(pool -> pool.getName().startsWith(poolNamePrefix))
				.forEach(pool -> pool.getMembers().get().forEach(member -> {
					final String host = pool.getName().substring(poolNamePrefix.length());
					final String[] addressParts = member.getName().split(":");
					final InetSocketAddress address = InetSocketAddress.createUnresolved(addressParts[0], Integer.valueOf(addressParts[1]));
					final PoolMemberDescription description = PoolMemberDescription
							.fromJsonish(member.getDescription())
							.orElse(new PoolMemberDescription());
					LOGGER.info("Registering existing route from F5 for host {} with target {}", host, address);
					routeRegistrar.insertRoute(
							host,
							address,
							description.getApplicationGuid(),
							description.getApplicationIndex(),
							description.getPrivateInstanceId());
				}));

	}

	public void registerRoute(RouteDetails route) {
		final String poolName = poolNamePrefix + route.getHost();

		final PoolDescription poolDescription = new PoolDescription(Instant.now(), Instant.now());
		final String poolDescriptionJsonish = poolDescription.toJsonish();

		final Pool pool = Pool.create()
				.name(poolName)
				.description(poolDescriptionJsonish)
				.monitor(Monitors.TCP_HALF_OPEN)
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
			final PoolMemberDescription poolMemberDescription = new PoolMemberDescription(route);
			client.addPoolMember(poolName, route.getAddress(), poolMemberDescription.toJsonish());
			LOGGER.info("Added pool member {} to pool {}", route.getAddress(), poolName);

			if (!poolCreated) {
				// If we didn't create the pool but added the pool member, update the modified fields in the
				// pool description
				updatePoolModifiedTimestamp(poolName);
				LOGGER.debug("Updated modified field on pool {}", poolName);
			}
		} catch (ConflictException e) {
			// Pool member already exists
			updatePoolMemberDescription(poolName, route);
		}
	}

	public void unregisterRoute(RouteDetails route) {
		final String poolName = poolNamePrefix + route.getHost();
		try {
			LOGGER.info("Removing pool member {} from pool {}", route.getAddress(), poolName);
			client.deletePoolMember(poolName, route.getAddress());
			updatePoolModifiedTimestamp(poolName);
			LOGGER.debug("Updated modified field on pool {}", poolName);
		} catch (ResourceNotFoundException e) {
			// Member was already removed
		}
	}

	private void updatePoolModifiedTimestamp(String poolName) {
		final Pool pool = client.getPool(poolName);
		final PoolDescription description = PoolDescription.fromJsonish(pool.getDescription()).orElse(new PoolDescription());
		description.setModified(Instant.now());
		client.updatePoolDescription(poolName, description.toJsonish());
	}

	private void updatePoolMemberDescription(String poolName, RouteDetails route) {
		final Pool pool = client.getPool(poolName);
		pool.getMembers().ifPresent(members -> members.stream()
			.filter(member -> member.getName().equals(route.getAddress().toString()))
			.findFirst()
			.ifPresent(member -> {
				final Optional<PoolMemberDescription> existingDescription = PoolMemberDescription.fromJsonish(member.getDescription());
				final PoolMemberDescription desiredDescription = new PoolMemberDescription(route);
				boolean update = true;
				if (existingDescription.isPresent()) {
					final PoolMemberDescription description = existingDescription.get();
					desiredDescription.setCreated(description.getCreated());
					desiredDescription.setModified(description.getModified());
					update = !desiredDescription.equals(description);
				}
				if (update) {
					desiredDescription.setModified(Instant.now());
					client.updatePoolMemberDescription(poolName, route.getAddress(), desiredDescription.toJsonish());
				}
			}));
	}

}
