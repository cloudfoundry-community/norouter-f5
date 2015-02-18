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
import cloudfoundry.norouter.f5.client.Pool;

import java.time.Instant;

/**
 * @author Mike Heath
 */
class Util {

	private Util() {}

	public static void updatePoolModifiedTimestamp(IControlClient client, String poolName) {
		final Pool pool = client.getPool(poolName);
		final PoolDescription description = PoolDescription.fromJsonish(pool.getDescription());
		description.setModified(Instant.now());
		client.updatePoolDescription(poolName, description.toJsonish());
	}

}
