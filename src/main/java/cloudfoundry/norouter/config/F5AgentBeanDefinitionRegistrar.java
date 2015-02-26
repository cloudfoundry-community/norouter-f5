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

import cloudfoundry.norouter.f5.Agent;
import cloudfoundry.norouter.f5.client.HttpClientIControlClient;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.env.Environment;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.util.StringUtils;

/**
 * @author Mike Heath
 */
class F5AgentBeanDefinitionRegistrar implements EnvironmentAware, ImportBeanDefinitionRegistrar {

	private Environment environment;

	@Override
	public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry) {
		for (int i = 0; true; i++) {
			final LtmProperties properties = extractProperties(i);
			if (properties == null) {
				break;
			}
			final AbstractBeanDefinition clientBeanDefinition = BeanDefinitionBuilder.genericBeanDefinition(F5AgentBeanDefinitionRegistrar.class)
					.setFactoryMethod("buildClient")
					.addConstructorArgValue(properties.url)
					.addConstructorArgValue(properties.user)
					.addConstructorArgValue(properties.password)
					.getBeanDefinition();

			final AbstractBeanDefinition agentBeanDefinition = BeanDefinitionBuilder.genericBeanDefinition(Agent.class)
					.addConstructorArgValue(properties.poolNamePrefix)
					.addConstructorArgValue(clientBeanDefinition)
					.addConstructorArgReference("routingTable")
					.getBeanDefinition();
			registry.registerBeanDefinition("f5Agent-" + i, agentBeanDefinition);
		}
	}

	static HttpClientIControlClient buildClient(String url, String user, String password) {
		return HttpClientIControlClient.create()
				.url(url)
				.user(user)
				.password(password)
				.skipVerifyTls(true) // TODO Make 'skipVerifyTls' configurable
				.build();
	}

	private LtmProperties extractProperties(int index) {
		final String prefix = "f5.ltms[" + index + "].";
		final String url = environment.getProperty(prefix + "url");
		if (StringUtils.isEmpty(url)) {
			return null;
		}
		final String poolNamePrefix = environment.getProperty(prefix + "poolNamePrefix");
		final String user = environment.getProperty(prefix + "user");
		final String password = environment.getProperty(prefix + "password");
		return new LtmProperties(poolNamePrefix, url, user, password);
	}

	@Override
	public void setEnvironment(Environment environment) {
		this.environment = environment;
	}

	static class LtmProperties {
		private final String poolNamePrefix;
		private final String url;
		private final String user;
		private final String password;

		public LtmProperties(String poolNamePrefix, String url, String user, String password) {
			this.poolNamePrefix = poolNamePrefix;
			this.url = url;
			this.user = user;
			this.password = password;
		}
	}
}
