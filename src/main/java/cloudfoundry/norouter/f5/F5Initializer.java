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

import cloudfoundry.norouter.config.F5Properties;
import cloudfoundry.norouter.config.F5RouterVipProperties;
import cloudfoundry.norouter.f5.client.IControlClient;
import cloudfoundry.norouter.f5.client.VirtualServer;
import cloudfoundry.norouter.f5.dropsonde.LoggingPoolPopulator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;
import org.stringtemplate.v4.ST;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * @author Mike Heath
 */
@Component
public class F5Initializer implements ApplicationListener<ContextRefreshedEvent> {

	private static final Logger LOGGER = LoggerFactory.getLogger(F5Initializer.class);

	@Autowired
	F5Properties properties;

	@Autowired
	F5RouterVipProperties vipProperties;

	@Autowired
	IControlClient client;

	@Autowired
	LoggingPoolPopulator loggingPoolPopulator;

	ST routerIRule() throws IOException {
		final ClassPathResource resource = new ClassPathResource("templates/irules/router.tcl.st");
		final String template = StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);
		return new ST(template, '`', '`');
	}

	ST loggingIRule() throws IOException {
		final ClassPathResource resource = new ClassPathResource("templates/irules/logging.tcl.st");
		final String template = StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);
		return new ST(template, '`', '`');
	}

	ST sessionAffinityIRule() throws IOException {
		final ClassPathResource resource = new ClassPathResource("templates/irules/session-affinity.tcl.st");
		final String template = StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);
		return new ST(template);
	}

	@Override
	public void onApplicationEvent(ContextRefreshedEvent event) {
		try {
			final String sessionAffinityIRule = sessionAffinityIRule().render();
			final String sessionAffinityIruleName = properties.getiRuleNamePrefix() + "session_affinity";
			LOGGER.info("Updating iRule {}", sessionAffinityIruleName);
			client.createOrUpdateIRule(sessionAffinityIruleName, sessionAffinityIRule);

			final String routerIRule = routerIRule()
					.add("poolNamePrefix", properties.getPoolNamePrefix())
					.render();
			final String routerIRuleName = properties.getiRuleNamePrefix() + "router";
			LOGGER.info("Updating iRule {}", routerIRuleName);
			client.createOrUpdateIRule(routerIRuleName, routerIRule);

			loggingPoolPopulator.updateLoggingPool();
			final String loggingPoolName = properties.getLoggingPoolName();
			final String loggingIRule = loggingIRule()
					.add("logging_pool", loggingPoolName)
					.add("ltm_id", properties.getLtmId())
					.render();
			final String loggingIRuleName = properties.getiRuleNamePrefix() + "logging";
			LOGGER.info("Updating iRule {} using pool {} for handling logging events", loggingIRuleName, loggingPoolName);
			client.createOrUpdateIRule(loggingIRuleName, loggingIRule);

			LOGGER.info("Updating virtual server {}/{}", vipProperties.getName(), vipProperties.getDestination());
			final VirtualServer.Builder routerBuilder = VirtualServer.create()
					.name(vipProperties.getName())
					.description(vipProperties.getDescription())
					.destination(vipProperties.getDestination())
					.addRule(sessionAffinityIruleName)
					.addRule(routerIRuleName)
					.addRule(loggingIRuleName);
			vipProperties.getRules().forEach(routerBuilder::addRule);
			routerBuilder
					.addProfile(VirtualServer.Profile.TCP_PROFILE)
					.addProfile(VirtualServer.Profile.HTTP_PROFILE);
			client.createOrUpdateVirtualServer(routerBuilder.build());
		} catch (IOException e) {
			throw new BeanInitializationException("Error initializing F5 LTM", e);
		}
	}
}

