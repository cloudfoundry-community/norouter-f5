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

import cloudfoundry.norouter.f5.ContextStartedListener;
import cloudfoundry.norouter.f5.RouteRegisterListener;
import cloudfoundry.norouter.f5.RouteUnregisterListener;
import cloudfoundry.norouter.f5.client.HttpClientIControlClient;
import cloudfoundry.norouter.f5.client.IControlClient;
import cloudfoundry.norouter.routingtable.RouteRegistrar;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author Mike Heath
 */
@Configuration
public class F5Config {

	private static final Logger LOGGER = LoggerFactory.getLogger(F5Config.class);

	@Value("${f5.poolNamePrefix:pool_cf_}")
	private String poolNamePrefix;

	@Bean
	HttpClientIControlClient iControlClient(
			@Value("${ssl.skip_cert_verify:true}") boolean skipVerifyTls,
	        @Value("${f5.url:slb-diz-dev-a.ldschurch.org}") String url,
	        @Value("${f5.user}") String user,
			@Value("${f5.password}") String password
	) {
		return HttpClientIControlClient.create()
				.skipVerifyTls(skipVerifyTls)
				.url(url)
				.user(user)
				.password(password)
				.build();
	}

	@Bean
	RouteRegisterListener f5routeRegisterListener(IControlClient client) {
		return new RouteRegisterListener(poolNamePrefix, client);
	}

	@Bean
	RouteUnregisterListener f5routeUnregisterListener(IControlClient client) {
		return new RouteUnregisterListener(poolNamePrefix, client);
	}

	@Bean
	ContextStartedListener f5contextStartedListener(IControlClient client, RouteRegistrar routeRegistrar) {
		return new ContextStartedListener(poolNamePrefix, client, routeRegistrar);
	}
}
