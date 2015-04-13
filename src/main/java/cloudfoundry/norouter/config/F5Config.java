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

import cloudfoundry.norouter.f5.client.HttpClientIControlClient;
import cloudfoundry.norouter.f5.client.IControlClient;
import cloudfoundry.norouter.routingtable.RouteRegistrar;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.StreamUtils;
import org.stringtemplate.v4.ST;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * @author Mike Heath
 */
@Configuration
@EnableConfigurationProperties(F5Properties.class)
@ComponentScan("cloudfoundry.norouter.f5")
public class F5Config implements InitializingBean {

	private static final Logger LOGGER = LoggerFactory.getLogger(F5Config.class);

	@Autowired
	F5Properties properties;

	@Autowired
	RouteRegistrar routeRegistrar;

	@Bean
	IControlClient iControlClient() {
		return HttpClientIControlClient.create()
				.url(properties.getUrl())
				.user(properties.getUser())
				.password(properties.getPassword())
				.skipVerifyTls(properties.isSkipTlsVerification())
				.build();
	}

//	@Bean
//	Agent agent() {
//		return new Agent(properties.getPoolNamePrefix(), iControlClient(), routeRegistrar);
//	}

	@Bean
	@Scope(BeanDefinition.SCOPE_PROTOTYPE)
	ST routerIRule() throws IOException {
		final ClassPathResource resource = new ClassPathResource("templates/irules/router.tcl.st");
		final String template = StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);
		return new ST(template, '`', '`');
	}

	@Bean
	@Scope(BeanDefinition.SCOPE_PROTOTYPE)
	ST loggingIRule() throws IOException {
		final ClassPathResource resource = new ClassPathResource("templates/irules/logging.tcl.st");
		final String template = StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);
		return new ST(template, '`', '`');
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		final String routerIRule = routerIRule()
				.add("poolNamePrefix", properties.getPoolNamePrefix())
				.render();
		final String routerIRuleName = properties.getiRuleNamePrefix() + "router";
		LOGGER.info("Updating iRule {}", routerIRuleName);
		iControlClient().createOrUpdateIRule(routerIRuleName, routerIRule);

		final String loggingPoolName = properties.getLoggingPoolName();
		final String loggingIRule = loggingIRule()
				.add("logging_pool", loggingPoolName)
				.add("ltm_id", properties.getLtmId())
				.render();
		final String loggingIRuleName = properties.getiRuleNamePrefix() + "logging";
		LOGGER.info("Updating iRule {} using pool {} for handling logging events", loggingIRuleName, loggingPoolName);
		iControlClient().createOrUpdateIRule(loggingIRuleName, loggingIRule);
	}

}
