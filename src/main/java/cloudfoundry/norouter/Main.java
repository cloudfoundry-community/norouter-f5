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
package cloudfoundry.norouter;

import cf.dropsonde.MetronClient;
import cf.dropsonde.MetronClientBuilder;
import cf.spring.NettyEventLoopGroupFactoryBean;
import cf.spring.PidFileFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.core.annotation.Order;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import java.io.IOException;
import java.net.InetSocketAddress;

/**
 * @author Mike Heath
 */
@SpringBootApplication
@EnableConfigurationProperties({MetronProperties.class, NorouterProperties.class})
@EnableScheduling
public class Main {

	@Autowired
	MetronProperties metronProperties;

	@Bean
	@Order(Integer.MAX_VALUE)
	@Qualifier("boss")
	NettyEventLoopGroupFactoryBean bossGroup() {
		return new NettyEventLoopGroupFactoryBean(1);
	}

	@Bean
	@ConditionalOnProperty("pidfile")
	PidFileFactory pidFile(@Value("${pidfile}") String pidfile) throws IOException {
		return new PidFileFactory(pidfile);
	}

	@Bean
	MetronClient metronClient() {
		return MetronClientBuilder
				.create("norouter")
				.metronAgent(new InetSocketAddress(metronProperties.getAddress(), metronProperties.getPort()))
				.build();
	}

	@Bean
	TaskScheduler taskScheduler() {
		return new ThreadPoolTaskScheduler();
	}

	public static void main(String[] args) {
		new SpringApplicationBuilder().sources(Main.class).build().run(args);
	}

}
