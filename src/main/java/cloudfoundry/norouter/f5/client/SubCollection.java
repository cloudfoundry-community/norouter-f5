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
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Optional;

/**
 * @author Mike Heath <elcapo@gmail.com>
 */
class SubCollection<T> extends JsonObject {

	private final URI link;
	private final Optional<Collection<T>> items;

	public SubCollection(
			@JsonProperty("link") URI link,
			@JsonProperty("items") Collection<T> items) {
		this.link = link;
		if (items == null) {
			this.items = Optional.empty();
		} else {
			items = Collections.unmodifiableCollection(new ArrayList<>(items));
			this.items = Optional.of(items);
		}
	}

	public URI getLink() {
		return link;
	}

	@JsonIgnore
	public Optional<Collection<T>> getItems() {
		return items;
	}

	@JsonProperty("isSubcomponent")
	boolean isSubcomponent() {
		return true;
	}

	@JsonProperty("items")
	Collection<T> getItemsForJson() {
		return items.orElse(null);
	}

}
