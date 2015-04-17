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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Optional;

/**
 * @author Mike Heath
 */
public class VirtualServer extends JsonObject {

	public static final String IP_PROTOCOL_TCP = "tcp";
	public static final String IP_PROTOCOL_UDP = "udp";

	private final String name;
	private final String description;
	private final String destination;
	private final String mask;
	private final String ipProtocol;
	private final Collection<String> rules;
	private final SourceAddressTranslation sourceAddressTranslation;
	private final SubCollection<Profile> profiles;
	private final String pool;

	public static Builder create() {
		return new Builder();
	}

	public static class Builder {

		private String name;
		private String description;
		private String destination;
		private String mask = "255.255.255.255";
		private String ipProtocol = "tcp";
		private Collection<String> rules = new ArrayList<>();
		private SourceAddressTranslation sourceAddressTranslation = SourceAddressTranslation.AUTOMAP;
		private Collection<Profile> profiles = new ArrayList<>();
		private String pool;

		public Builder name(String name) {
			this.name = name;
			return this;
		}

		public Builder description(String description) {
			this.description = description;
			return this;
		}

		public Builder destination(String destination) {
			this.destination = destination;
			return this;
		}

		public Builder mask(String mask) {
			this.mask = mask;
			return this;
		}

		public Builder ipProtocol(String ipProtocol) {
			this.ipProtocol = ipProtocol;
			return this;
		}

		public Builder addRule(String ruleName) {
			rules.add(ruleName);
			return this;
		}

		public Builder sourceAddressTranslation(String type) {
			this.sourceAddressTranslation = new SourceAddressTranslation(type);
			return this;
		}

		public Builder pool(String pool) {
			this.pool = pool;
			return this;
		}

		public Builder addProfile(Profile profile) {
			profiles.add(profile);
			return this;
		}

		public Builder addProfile(String profile) {
			profiles.add(new Profile(Profile.KIND_PROFILE, profile));
			return this;
		}

		public VirtualServer build() {
			return new VirtualServer(this);
		}

	}

	@JsonCreator
	VirtualServer(
			@JsonProperty("name") String name,
			@JsonProperty("description") String description,
			@JsonProperty("destination") String destination,
			@JsonProperty("mask") String mask,
			@JsonProperty("ipProtocol") String ipProtocol,
			@JsonProperty("rules") Collection<String> rules,
			@JsonProperty("sourceAddressTranslation") SourceAddressTranslation sourceAddressTranslation,
			@JsonProperty("profilesReference") SubCollection<Profile> profiles,
			@JsonProperty("pool") String pool) {
		this.name = name;
		this.description = description;
		this.destination = destination;
		this.mask = mask;
		this.ipProtocol = ipProtocol;
		this.rules = rules == null ? Collections.emptyList() : Collections.unmodifiableCollection(new ArrayList<>(rules));
		this.sourceAddressTranslation = sourceAddressTranslation;
		this.profiles = profiles;
		this.pool = pool;
		validate();
	}

	private VirtualServer(Builder builder) {
		this.name = builder.name;
		this.description = builder.description;
		this.destination = builder.destination;
		this.mask = builder.mask;
		this.ipProtocol = builder.ipProtocol;
		this.rules = Collections.unmodifiableCollection(new ArrayList<>(builder.rules));
		this.sourceAddressTranslation = builder.sourceAddressTranslation;
		this.profiles = new SubCollection<>(null, builder.profiles);
		this.pool = builder.pool;
		validate();
	}

	private void validate() {
		if (name == null || name.trim().length() == 0) {
			throw new IllegalArgumentException("name can not be empty");
		}
	}

	public String getName() {
		return name;
	}

	public String getDescription() {
		return description;
	}

	public String getDestination() {
		return destination;
	}

	public String getMask() {
		return mask;
	}

	public String getIpProtocol() {
		return ipProtocol;
	}

	public Collection<String> getRules() {
		return rules;
	}

	public SourceAddressTranslation getSourceAddressTranslation() {
		return sourceAddressTranslation;
	}

	@JsonIgnore
	public Optional<Collection<Profile>> getProfiles() {
		return profiles.getItems();
	}

	@JsonProperty("profilesReference")
	SubCollection<Profile> getProfilesReference() {
		return profiles;
	}

	public String getPool() {
		return pool;
	}

	public static class SourceAddressTranslation extends JsonObject {
		public static final String TYPE_AUTOMAP = "automap";

		public static final SourceAddressTranslation AUTOMAP = new SourceAddressTranslation(TYPE_AUTOMAP);

		private final String type;

		@JsonCreator
		public SourceAddressTranslation(
				@JsonProperty("type") String type
		) {
			this.type = type;
		}

		public String getType() {
			return type;
		}
	}

	public static class Profile extends JsonObject {
		private static final String KIND_PROFILE = "ltm:virtual:profile";
		private static final String PROFILE_TCP = "tcp";
		private static final String PROFILE_HTTP = "http";

		public static Profile HTTP_PROFILE = new Profile(KIND_PROFILE, PROFILE_HTTP);
		public static Profile TCP_PROFILE = new Profile(KIND_PROFILE, PROFILE_TCP);

		private final String kind;
		private final String name;

		@JsonCreator
		public Profile(
				@JsonProperty("kind") String kind,
				@JsonProperty("name") String name) {
			this.kind = kind;
			this.name = name;
		}

		public String getKind() {
			return kind;
		}

		public String getName() {
			return name;
		}
	}
}
