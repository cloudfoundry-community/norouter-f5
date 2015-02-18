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
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import java.time.Instant;

/**
 * @author Mike Heath
 */
public class PoolDescription extends JsonObject {

	private Instant created;
	private Instant modified;

	@JsonCreator
	public PoolDescription(
			@JsonProperty("created") Instant created,
			@JsonProperty("modified") Instant modified) {
		this.created = created;
		this.modified = modified;
	}

	@JsonDeserialize()
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

	public String toJsonish() {
		return Json.escape(Json.toJson(this));
	}

	public static PoolDescription fromJsonish(String jsonish) {
		return Json.fromJson(PoolDescription.class, Json.unescape(jsonish));
	}
}
