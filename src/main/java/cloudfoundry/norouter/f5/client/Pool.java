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

import cf.common.JsonObject;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;
import java.util.Optional;

/**
 * @author Mike Heath <elcapo@gmail.com>
 */
public class Pool extends JsonObject {

	private final String name;
	private final String description;
	private final String monitor;
	private final SubCollection<PoolMember> poolMembers;
	private final Integer reselectTries;

	public static class Builder {
		private String name;
		private String description;
		private String monitor;
		private Collection<PoolMember> poolMembers = new ArrayList<>();
		private Integer reselectTries;

		public Builder name(String name) {
			this.name = name;
			return this;
		}

		public Builder description(String description) {
			this.description = description;
			return this;
		}

		public Builder addMember(InetSocketAddress address) {
			return addMember(address, null);
		}

		public Builder addMember(InetSocketAddress address, String description) {
			return addMember(address.getHostString(), address.getPort(), description);
		}

		public Builder addMember(String host, int port) {
			return addMember(host, port, null);
		}

		public Builder addMember(String host, int port, String description) {
			poolMembers.add(new PoolMember(host + ":" + port, description));
			return this;
		}

		public Builder monitor(String monitor) {
			this.monitor = monitor;
			return this;
		}

		public Builder reselectTries(Integer reselectTries) {
			this.reselectTries = reselectTries;
			return this;
		}

		public Pool build() {
			return new Pool(this);
		}
	}

	public static Builder create() {
		return new Builder();
	}

	private Pool(Builder builder) {
		name = Objects.requireNonNull(builder.name, "name is a required parameter");
		description = builder.description;
		monitor = builder.monitor;
		poolMembers = new SubCollection<>(null, builder.poolMembers);
		reselectTries = builder.reselectTries;
	}

	@JsonCreator
	Pool(
			@JsonProperty("name") String name,
			@JsonProperty("description") String description,
			@JsonProperty("monitor") String monitor,
			@JsonProperty("membersReference") SubCollection<PoolMember> poolMembers,
	        @JsonProperty("reselectTries") Integer reselectTries
	) {
		this.name = name;
		this.description = description;
		this.monitor = monitor;
		this.poolMembers = poolMembers;
		this.reselectTries = reselectTries;
	}

	public String getName() {
		return name;
	}

	public String getDescription() {
		return description;
	}

	public String getMonitor() {
		return monitor;
	}

	public Integer getReselectTries() {
		return reselectTries;
	}

	@JsonIgnore
	public Optional<Collection<PoolMember>> getMembers() {
		return poolMembers.getItems();
	}

	@JsonProperty("membersReference")
	SubCollection<PoolMember> getMembersReference() {
		return poolMembers;
	}
}
