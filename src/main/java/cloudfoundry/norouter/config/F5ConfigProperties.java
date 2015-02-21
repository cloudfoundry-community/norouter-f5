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

package cloudfoundry.norouter.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @author Mike Heath
 */
@ConfigurationProperties(prefix = "f5")
public class F5ConfigProperties {

	private String poolNamePrefix = "pool_cf_";

	public String getPoolNamePrefix() {
		return poolNamePrefix;
	}

	public void setPoolNamePrefix(String poolNamePrefix) {
		this.poolNamePrefix = poolNamePrefix;
	}
}
