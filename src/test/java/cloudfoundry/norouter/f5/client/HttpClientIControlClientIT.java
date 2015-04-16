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

package cloudfoundry.norouter.f5.client;

import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.net.InetSocketAddress;
import java.util.Collection;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

/**
 * @author Mike Heath <elcapo@gmail.com>
 */
public class HttpClientIControlClientIT {

	private IControlClient client;
	private HttpClientIControlClient.Builder builder;

	@BeforeClass
	public void setup() {
		final String ltmHost = System.getenv("LTM_URL");
		if (ltmHost == null) {
			throw new IllegalStateException("You MUST set the environment variable 'LTM_URL' with the HTTPS URL " +
					"(including credentials) to run the integration tests.");
		}
		builder = HttpClientIControlClient.create()
				.url(ltmHost)
				.skipVerifyTls(true);
		client = builder.build();
	}

	@AfterClass
	public void tearDown() {
		client.close();
	}

	@Test(expectedExceptions = AuthorizationRequiredException.class)
	public void badCredentials() {
		try(final IControlClient client = builder.user("baduser").password("badpassword").build()) {
			client.getAllPools();
		}
	}

	@Test
	public void createAndDeleteEmptyPool() {
		final String poolName = "__Rest-Client-Test-" + UUID.randomUUID().toString();
		final String description = "If you're reading this, please delete this pool.";
		final Pool pool = Pool.create().name(poolName).description(description).build();

		final Pool createdPool = client.createPool(pool);
		assertEquals(createdPool.getName(), poolName);
		assertEquals(createdPool.getDescription(), description);

		final Pool fetchedPool = client.getPool(poolName);
		assertEquals(fetchedPool.getName(), poolName);
		assertEquals(fetchedPool.getDescription(), description);

		final Collection<Pool> allPools = client.getAllPools();
		final Optional<Pool> poolFromAllPools = allPools.stream().filter(p -> p.getName().equals(poolName)).findFirst();
		assertTrue(poolFromAllPools.isPresent());

		client.deletePool(poolName);

		try {
			client.getPool(poolName);
			fail("Pool was not deleted!");
		} catch (ResourceNotFoundException e) {
			// Success!!
		}
	}

	@Test
	public void poolMemberCreateAndDelete() {
		final InetSocketAddress poolMember = InetSocketAddress.createUnresolved("1.2.3.4", 80);
		final String poolName = "__Rest-Client-Test-" + UUID.randomUUID().toString();
		final String description = "If you're reading this, please delete this pool.";
		final Pool pool = Pool.create()
				.name(poolName)
				.description(description)
				.addMember(poolMember)
				.build();

		client.createPool(pool);

		// Load pool and make sure it has the provided member
		final Pool createdPool = client.getPool(poolName);
		assertTrue(createdPool.getMembers().isPresent());
		{
			final Collection<PoolMember> memberCollection = createdPool.getMembers().get();
			assertEquals(memberCollection.size(), 1);
			assertPoolMemberPresent(memberCollection, poolMember.toString());
		}

		// Add a pool member
		final InetSocketAddress secondPoolMember = InetSocketAddress.createUnresolved("192.168.254.253", 8080);
		client.addPoolMember(poolName, secondPoolMember);

		final Pool poolWithSecondMember = client.getPool(poolName);
		{
			final Collection<PoolMember> memberCollection = poolWithSecondMember.getMembers().get();
			assertEquals(memberCollection.size(), 2);
			assertPoolMemberPresent(memberCollection, secondPoolMember.toString());
		}

		// Remove second pool member
		client.deletePoolMember(poolName, secondPoolMember);

		final Pool poolWithoutSecondMember = client.getPool(poolName);
		assertTrue(poolWithoutSecondMember.getMembers().isPresent());
		{
			final Collection<PoolMember> memberCollection = poolWithoutSecondMember.getMembers().get();
			assertEquals(memberCollection.size(), 1);
			assertPoolMemberPresent(memberCollection, poolMember.toString());
		}

		client.deletePool(poolName);
	}

	@Test(expectedExceptions = ConflictException.class)
	public void createTwoPoolsWithSameName() {
		final String poolName = "__Rest-Client-Test-" + UUID.randomUUID().toString();
		final String description = "If you're reading this, please delete this pool.";
		final Pool pool = Pool.create()
				.name(poolName)
				.description(description)
				.build();

		client.createPool(pool);
		try {
			client.createPool(pool);
			fail("Attempt to create second pool should have been a conflict");
		} finally {
			client.deletePool(poolName);
		}

	}

	@Test(expectedExceptions = ResourceNotFoundException.class)
	public void badPoolNameThrowsResourceNotFoundException() {
		client.getPool("YoDawgIHeardYouLikePools");
	}

	@Test(expectedExceptions = ResourceNotFoundException.class)
	public void deletePoolMemberThatDoesNotExist() {
		client.deletePoolMember("somepoolthatbetternotexist", "0.0.0.0:1");
	}

	@Test
	public void disablePoolMember() {
		final String poolName = "__Rest-Client-Test-" + UUID.randomUUID().toString();
		final String description = "If you're reading this, please delete this pool.";
		final Pool pool = Pool.create()
				.name(poolName)
				.description(description)
				.build();
		client.createPool(pool);
		try {
			final InetSocketAddress poolMember = InetSocketAddress.createUnresolved("1.2.2.1", 80);

			client.addPoolMember(poolName, poolMember);

			final PoolMember disabledPoolMember = client.disablePoolMember(poolName, poolMember);
			assertFalse(disabledPoolMember.isEnabled());
		} finally {
			client.deletePool(poolName);
		}
	}

	@Test
	public void createIRule() {
		final String name = "__TestIRule-" + UUID.randomUUID().toString();
		final String body = "# If you're reading this, delete this iRule.\nwhen HTTP_REQUEST {}";
		try {
			final IRule iRule = client.createIRule(name, body);
			assertEquals(iRule.getName(), name);
			assertEquals(iRule.getBody(), body);
		} finally {
			client.deleteIRule(name);
		}
	}

	@Test
	public void createVirtualServer() {
		final String name = "aa__TestVirtualServer-" + UUID.randomUUID().toString();
		final String description = "If you are reading this, please delete this virtual server.";
		final String destination = "1"
				+ "." + ThreadLocalRandom.current().nextInt(255)
				+ "." + ThreadLocalRandom.current().nextInt(255)
				+ "." + ThreadLocalRandom.current().nextInt(255)
				+ ":80";
		// This rule should be available on all LTMs
		final String systemRule = "_sys_https_redirect";

		try {
			final VirtualServer virtualServer = VirtualServer.create()
					.name(name)
					.description(description)
					.destination(destination)
					.addRule(systemRule)
					.addProfile(VirtualServer.Profile.TCP_PROFILE)
					.addProfile(VirtualServer.Profile.HTTP_PROFILE)
					.build();

			final VirtualServer createdVirtualServer = client.createVirtualServer(virtualServer);
			assertEquals(createdVirtualServer.getName(), name);
			assertEquals(createdVirtualServer.getDescription(), description);
			assertTrue(createdVirtualServer.getDestination().contains(destination));
			assertTrue(createdVirtualServer.getRules().stream()
					.filter(rule -> rule.contains(systemRule))
					.findFirst()
					.isPresent());
		} finally {
			try {
				client.deleteVirtualServer(name);
			} catch (Throwable t) {
				t.printStackTrace();
			}
		}
	}

	private PoolMember assertPoolMemberPresent(Collection<PoolMember> poolMembers, String address) {
		final Optional<PoolMember> first = poolMembers.stream()
				.filter(p -> p.getName().equals(address))
				.findFirst();
		assertTrue(first.isPresent());
		return first.get();
	}
}
