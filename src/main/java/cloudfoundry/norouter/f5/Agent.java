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
import cloudfoundry.norouter.f5.client.PoolMember;
import cloudfoundry.norouter.f5.client.ResourceNotFoundException;
import cloudfoundry.norouter.routingtable.RouteDetails;
import cloudfoundry.norouter.routingtable.RouteRegisterEvent;
import cloudfoundry.norouter.routingtable.RouteRegistrar;
import cloudfoundry.norouter.routingtable.RouteUnregisterEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.core.Ordered;

import java.net.InetSocketAddress;
import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * @author Mike Heath
 */
// TODO Should we loggregate when pool members are added/removed?
// TODO If we throw an exception adding a route, what do we do?
public class Agent implements ApplicationListener<ApplicationEvent>, Ordered, AutoCloseable {

	private static final Logger LOGGER = LoggerFactory.getLogger(Agent.class);

	private static final Duration POOL_STALE_TIME = Duration.ofMinutes(5);

	private final String poolNamePrefix;
	private final IControlClient client;
	private final RouteRegistrar routeRegistrar;

	private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

	public Agent(String poolNamePrefix, IControlClient client, RouteRegistrar routeRegistrar) {
		this.poolNamePrefix = poolNamePrefix;
		this.client = client;
		this.routeRegistrar = routeRegistrar;
		scheduler.scheduleAtFixedRate(this::removeStalePools, 30, 30, TimeUnit.MINUTES);
	}

	@Override
	public void close() throws Exception {
		scheduler.shutdownNow().forEach(Runnable::run);
	}

	public void removeStalePools() {
		LOGGER.info("Checking for stale pools prefixed with {}", poolNamePrefix);
		client.getAllPools(true).stream()
				.filter(pool -> pool.getName().startsWith(poolNamePrefix))
				.filter(pool -> !pool.getMembers().isPresent() || pool.getMembers().get().size() == 0)
				.filter(pool -> {
					// Filter out non stale pools.
					final Optional<PoolDescription> description = PoolDescription.fromJsonish(pool.getDescription());
					if (description.isPresent()) {
						final Duration timeSinceModified = Duration.between(description.get().getModified(), Instant.now());
						return POOL_STALE_TIME.compareTo(timeSinceModified) < 0;
					}
					return false;
				})
				.forEach(pool -> safeDeletePool(pool.getName()));
	}

	public void populateRouteRegistrar() {
		client.getAllPools(true).stream()
				.filter(pool -> pool.getName().startsWith(poolNamePrefix))
				.forEach(pool -> pool.getMembers().ifPresent(members ->
						members.forEach(member -> {
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
						})));
	}

	public void registerRoute(RouteDetails route) {
		final String poolName = poolNamePrefix + route.getHost();

		final PoolDescription poolDescription = new PoolDescription(Instant.now(), Instant.now());
		final String poolDescriptionJsonish = poolDescription.toJsonish();

		final Pool pool = Pool.create()
				.name(poolName)
				.description(poolDescriptionJsonish)
						// TODO Provide mechanism to make monitor configurable
				.monitor(Monitors.TCP_HALF_OPEN)
						// TODO Make reselect tries configurable
				.reselectTries(3)
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

	public void unregisterRoute(RouteUnregisterEvent unregisterEvent) {
		final String poolName = poolNamePrefix + unregisterEvent.getHost();
		final InetSocketAddress poolMember = unregisterEvent.getAddress();

		// Disable the pool member
		LOGGER.info("Disabling pool member () from pool {}", poolMember, poolName);
		client.disablePoolMember(poolName, poolMember);

		// Delete the pool member one minute later
		// TODO Check connection count and delete pool member sooner if it doesn't have any connections
		scheduler.schedule(() -> {
			boolean removedPoolMember = false;
			try {
				LOGGER.info("Removing pool member {} from pool {}", poolMember, poolName);
				client.deletePoolMember(poolName, poolMember);
				removedPoolMember = true;
			} catch (ResourceNotFoundException e) {
				// The pool member was already removed
			}

			boolean deletedPool = false;
			try {
				if (unregisterEvent.isLast()) {
					deletedPool = safeDeletePool(poolName);
				}
				if (!deletedPool && removedPoolMember) {
					updatePoolModifiedTimestamp(poolName);
					LOGGER.debug("Updated modified field on pool {}", poolName);
				}
			} catch (ResourceNotFoundException e) {
				// The pool was already removed
			}
		}, 1, TimeUnit.MINUTES);
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

	private boolean safeDeletePool(String name) {
		try {
			final Pool pool = client.getPool(name);
			final Optional<Collection<PoolMember>> members = pool.getMembers();
			if (members.isPresent() && members.get().size() > 0) {
				LOGGER.info("Can not delete pool {} because it still has pool members.", name);
				return false;
			}
			final Optional<PoolDescription> description = PoolDescription.fromJsonish(pool.getDescription());
			if (description.isPresent()) {
				client.deletePool(name);
				LOGGER.info("Deleted pool {}", name);
				return true;
			} else {
				LOGGER.info("Can not delete pool {} because it has a missing/invalid description.", name);
			}
			return false;
		} catch (ResourceNotFoundException e) {
			return false;
		}
	}

	@Override
	public void onApplicationEvent(ApplicationEvent event) {
		if (event instanceof RouteRegisterEvent) {
			registerRoute((RouteDetails) event);
		} else if (event instanceof RouteUnregisterEvent) {
			// TODO Differentiate unregister events from timeout events
			unregisterRoute((RouteUnregisterEvent) event);
		} else if (event instanceof ContextRefreshedEvent) {
			removeStalePools();
			populateRouteRegistrar();
		}
	}

	@Override
	public int getOrder() {
		return 0;
	}

}
