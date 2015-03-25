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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

/**
 * @author Mike Heath <elcapo@gmail.com>
 */
public abstract class AbstractIControlClient implements IControlClient {

	protected static final String EXPAND_SUBCOLLECTIONS = "?expandSubcollections=true";
	protected static final String LTM_URI = "/mgmt/tm/ltm";
	protected static final String IRULE_URI = LTM_URI + "/rule";
	protected static final String POOL_URI = LTM_URI + "/pool";


	protected final URI address;

	protected final ObjectMapper mapper = new ObjectMapper();
	protected final JsonNode DISABLE_POOL_MEMBER_BODY = mapper.createObjectNode()
			.put("session", "user-disabled");

	protected AbstractIControlClient(URI address) {
		this.address = address;
	}

	@Override
	public IRule createIRule(String name, String body) {
		final JsonNode createdRule = postResource(IRULE_URI, new IRule(name, body));
		return readValue(createdRule, IRule.class);
	}

	@Override
	public IRule getIRule(String name) throws ResourceNotFoundException {
		final String uri = IRULE_URI + "/" + name;
		return readValue(getResource(uri), IRule.class);
	}

	@Override
	public IRule updateIRule(String name, String body) {
		final String uri = IRULE_URI + "/" + name;
		final JsonNode updatedRule = putResource(uri, new IRule(name, body));
		return readValue(updatedRule, IRule.class);
	}

	@Override
	public void deleteIRule(String name) {
		final String uri = IRULE_URI + "/" + name;
		deleteResource(uri);
	}

	@Override
	public Pool createPool(Pool pool) {
		final JsonNode createdPool = postResource(POOL_URI, pool);
		return readValue(createdPool, Pool.class);
	}

	@Override
	public Pool getPool(String name) {
		final String uri = POOL_URI + "/" + name + EXPAND_SUBCOLLECTIONS;
		final JsonNode resource = getResource(uri);
		return readValue(resource, Pool.class);
	}

	@Override
	public void deletePool(String name) {
		final String uri = POOL_URI + "/" + name;
		deleteResource(uri);
	}

	@Override
	public Collection<Pool> getAllPools() {
		return getAllPools(false);
	}

	@Override
	public Collection<Pool> getAllPools(boolean fetchPoolMembers) {
		final String uri = POOL_URI + (fetchPoolMembers ? EXPAND_SUBCOLLECTIONS : "");
		final JsonNode resource = getResource(uri);
		final JsonNode items = resource.get("items");
		final Collection<Pool> pools = new ArrayList<>();
		items.elements().forEachRemaining(node -> pools.add(readValue(node, Pool.class)));
		return pools;
	}

	@Override
	public PoolMember addPoolMember(String poolName, InetSocketAddress poolMember) {
		return addPoolMember(poolName, poolMember, null);
	}

	@Override
	public PoolMember addPoolMember(String poolName, InetSocketAddress poolMember, String description) {
		return addPoolMember(poolName, poolMember.toString(), description);
	}

	@Override
	public PoolMember addPoolMember(String poolName, String poolMember) {
		return addPoolMember(poolName, poolMember, null);
	}

	@Override
	public PoolMember addPoolMember(String poolName, String poolMember, String description) {
		final PoolMember member = new PoolMember(poolMember, description);
		final JsonNode resource = postResource(membersUri(poolName), member);
		return readValue(resource, PoolMember.class);
	}

	@Override
	public void deletePoolMember(String poolName, InetSocketAddress poolMember) {
		deletePoolMember(poolName, poolMember.toString());
	}

	@Override
	public void deletePoolMember(String poolName, String poolMember) {
		deleteResource(membersUri(poolName) + "/" + poolMember);
	}

	@Override
	public Pool updatePoolDescription(String name, String description) {
		final String uri = POOL_URI + "/" + name;
		final JsonNode resource = putResource(uri, Collections.singletonMap("description", description));
		return readValue(resource, Pool.class);
	}

	@Override
	public PoolMember updatePoolMemberDescription(String poolName, InetSocketAddress member, String description) {
		final String uri = membersUri(poolName) + "/" + member.toString();
		final JsonNode resource = putResource(uri, Collections.singletonMap("description", description));
		return readValue(resource, PoolMember.class);
	}

	@Override
	public PoolMember disablePoolMember(String poolName, InetSocketAddress member) {
		final String uri = membersUri(poolName) + "/" + member.toString();
		final JsonNode resource = putResource(uri, DISABLE_POOL_MEMBER_BODY);
		return readValue(resource, PoolMember.class);
	}

	protected abstract JsonNode getResource(String uri);
	protected abstract JsonNode postResource(String uri, Object resource);
	protected abstract JsonNode putResource(String uri, Object resource);
	protected abstract void deleteResource(String uri);

	protected void validateResponse(int statusCode, String reason, String body, int... expectedStatusCodes) {
		for (int code : expectedStatusCodes) {
			if (code == statusCode) {
				return;
			}
		}
		switch (statusCode) {
			case 401:
				throw new AuthorizationRequiredException(reason);
			case 404:
				throw new ResourceNotFoundException(reason);
			case 409:
				throw new ConflictException(reason);
			default:
				throw new IControlException("Unexpected response: " + statusCode + " " + reason + "(" + body + ")");
		}
	}

	private <T> T readValue(JsonNode node, Class<T> type) {
		try {
			return mapper.readValue(node.traverse(), type);
		} catch (IOException e) {
			throw new JsonException(e);
		}
	}

	private String membersUri(String poolName) {
		return POOL_URI + "/" + poolName + "/members";
	}

}
