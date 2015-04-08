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

import cf.dropsonde.MetronClient;
import cloudfoundry.norouter.f5.Agent;
import cloudfoundry.norouter.f5.client.HttpClientIControlClient;
import cloudfoundry.norouter.f5.client.IControlClient;
import cloudfoundry.norouter.f5.dropsonde.LineEventToMetronServer;
import cloudfoundry.norouter.routingtable.RouteRegistrar;
import io.netty.channel.EventLoopGroup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
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

	@Bean
	Agent agent() {
		return new Agent(properties.getPoolNamePrefix(), iControlClient(), routeRegistrar);
	}

	@Bean
	LineEventToMetronServer lineEventServer(
			@Qualifier("boss") EventLoopGroup bossEventLoop,
			@Qualifier("worker") EventLoopGroup workerEventLoop,
			RouteRegistrar routeRegistrar,
	        MetronClient metronClient
	) {
		return new LineEventToMetronServer(bossEventLoop, workerEventLoop, routeRegistrar, metronClient);
	}

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
		updateIRule(properties.getiRuleNamePrefix() + "router", routerIRule);

		final String loggingIRule = loggingIRule()
				.add("logging_pool", properties.getPoolNamePrefix() + "_norouter_loggers")
				.add("ltm_id", properties.getLtmId())
				.render();
		updateIRule(properties.getiRuleNamePrefix() + "logging", loggingIRule);
	}

	private void updateIRule(String name, String rule) {
		LOGGER.info("Updating iRule {}", name);
		iControlClient().createOrUpdateIRule(name, rule);
	}
}
