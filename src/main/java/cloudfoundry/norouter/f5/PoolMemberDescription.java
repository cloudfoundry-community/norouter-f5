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

import cf.common.JsonObject;
import cloudfoundry.norouter.routingtable.RouteDetails;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * @author Mike Heath
 */
public class PoolMemberDescription extends JsonObject {

	private static final String PROPERTY_APPLICATION_GUID = "application_guid";
	private static final String PROPERTY_APPLICATION_INDEX = "application_index";
	private static final String PROPERTY_PRIVATE_INSTANCE_ID = "private_instance_id";

	private Instant created;
	private Instant modified;

	private UUID applicationGuid;
	private Integer applicationIndex;
	private String privateInstanceId;

	public PoolMemberDescription() {}

	public PoolMemberDescription(RouteDetails route) {
		created = modified = Instant.now();
		setApplicationGuid(route.getApplicationGuid());
		setApplicationIndex(route.getApplicationIndex());
		setPrivateInstanceId(route.getPrivateInstanceId());
	}

	public Instant getCreated() {
		return created;
	}

	public void setCreated(Instant created) {
		this.created = created;
	}

	public Instant getModified() {
		return modified;
	}

	public void setModified(Instant modified) {
		this.modified = modified;
	}

	@JsonProperty(PROPERTY_APPLICATION_GUID)
	public UUID getApplicationGuid() {
		return applicationGuid;
	}

	@JsonProperty(PROPERTY_APPLICATION_GUID)
	public void setApplicationGuid(UUID applicationGuid) {
		this.applicationGuid = applicationGuid;
	}

	@JsonProperty(PROPERTY_APPLICATION_INDEX)
	public Integer getApplicationIndex() {
		return applicationIndex;
	}

	public void setApplicationIndex(Integer applicationIndex) {
		this.applicationIndex = applicationIndex;
	}

	@JsonProperty(PROPERTY_PRIVATE_INSTANCE_ID)
	public String getPrivateInstanceId() {
		return privateInstanceId;
	}

	public void setPrivateInstanceId(String privateInstanceId) {
		this.privateInstanceId = privateInstanceId;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		PoolMemberDescription that = (PoolMemberDescription) o;

		if (applicationGuid != null ? !applicationGuid.equals(that.applicationGuid) : that.applicationGuid != null)
			return false;
		if (applicationIndex != null ? !applicationIndex.equals(that.applicationIndex) : that.applicationIndex != null)
			return false;
		if (created != null ? !created.equals(that.created) : that.created != null) return false;
		if (modified != null ? !modified.equals(that.modified) : that.modified != null) return false;
		if (privateInstanceId != null ? !privateInstanceId.equals(that.privateInstanceId) : that.privateInstanceId != null)
			return false;

		return true;
	}

	@Override
	public int hashCode() {
		int result = created != null ? created.hashCode() : 0;
		result = 31 * result + (modified != null ? modified.hashCode() : 0);
		result = 31 * result + (applicationGuid != null ? applicationGuid.hashCode() : 0);
		result = 31 * result + (applicationIndex != null ? applicationIndex.hashCode() : 0);
		result = 31 * result + (privateInstanceId != null ? privateInstanceId.hashCode() : 0);
		return result;
	}

	public String toJsonish() {
		return Json.toJsonish(this);
	}

	public static Optional<PoolMemberDescription> fromJsonish(String jsonish) {
		return Json.fromJsonish(PoolMemberDescription.class, jsonish);
	}

}
