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

import java.net.URI;

/**
 * @author Mike Heath
 */
@ConfigurationProperties("f5")
public class F5Properties {

	private URI url;
	private String user;
	private String password;

	private boolean skipTlsVerification = true;

	private String poolNamePrefix = "cf-pool_";
	private String iRuleNamePrefix = "cf-irule_";

	private String ltmId = "f5.ltmId";

	public String getUser() {
		return user;
	}

	public void setUser(String user) {
		this.user = user;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public String getPoolNamePrefix() {
		return poolNamePrefix;
	}

	public void setPoolNamePrefix(String poolNamePrefix) {
		this.poolNamePrefix = poolNamePrefix;
	}

	public String getiRuleNamePrefix() {
		return iRuleNamePrefix;
	}

	public void setiRuleNamePrefix(String iRuleNamePrefix) {
		this.iRuleNamePrefix = iRuleNamePrefix;
	}

	public URI getUrl() {
		return url;
	}

	public void setUrl(URI url) {
		this.url = url;
	}

	public boolean isSkipTlsVerification() {
		return skipTlsVerification;
	}

	public void setSkipTlsVerification(boolean skipTlsVerification) {
		this.skipTlsVerification = skipTlsVerification;
	}


	public String getLtmId() {
		return ltmId;
	}

	public void setLtmId(String ltmId) {
		this.ltmId = ltmId;
	}
}
