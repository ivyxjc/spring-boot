/*
 * Copyright 2012-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.autoconfigure.mongo;

import com.mongodb.MongoClient;
import com.mongodb.MongoClientOptions;
import com.mongodb.client.MongoClients;
import org.junit.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.net.SocketFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link MongoAutoConfiguration}.
 *
 * @author Dave Syer
 * @author Stephane Nicoll
 */
public class MongoAutoConfigurationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(MongoAutoConfiguration.class));

	@Test
	public void clientExists() {
		this.contextRunner.run((context) -> assertThat(context).hasSingleBean(MongoClient.class));
	}

	@Test
	public void optionsAdded() {
		this.contextRunner.withUserConfiguration(OptionsConfig.class).run(
				(context) -> assertThat(context.getBean(MongoClient.class).getMongoClientOptions().getSocketTimeout())
						.isEqualTo(300));
	}

	@Test
	public void optionsAddedButNoHost() {
		this.contextRunner.withUserConfiguration(OptionsConfig.class).run(
				(context) -> assertThat(context.getBean(MongoClient.class).getMongoClientOptions().getSocketTimeout())
						.isEqualTo(300));
	}

	@Test
	public void optionsSslConfig() {
		this.contextRunner.withUserConfiguration(SslOptionsConfig.class).run((context) -> {
			assertThat(context).hasSingleBean(MongoClient.class);
			MongoClient mongo = context.getBean(MongoClient.class);
			MongoClientOptions options = mongo.getMongoClientOptions();
			assertThat(options.isSslEnabled()).isTrue();
			assertThat(options.getSocketFactory()).isSameAs(context.getBean("mySocketFactory"));
		});
	}

	@Test
	public void doesNotCreateMongoClientWhenAlreadyDefined() {
		this.contextRunner.withUserConfiguration(FallbackMongoClientConfig.class).run((context) -> {
			assertThat(context).doesNotHaveBean(MongoClient.class);
			assertThat(context).hasSingleBean(com.mongodb.client.MongoClient.class);
		});
	}

	@Configuration
	static class OptionsConfig {

		@Bean
		public MongoClientOptions mongoOptions() {
			return MongoClientOptions.builder().socketTimeout(300).build();
		}

	}

	@Configuration
	static class SslOptionsConfig {

		@Bean
		public MongoClientOptions mongoClientOptions() {
			return MongoClientOptions.builder().sslEnabled(true).socketFactory(mySocketFactory()).build();
		}

		@Bean
		public SocketFactory mySocketFactory() {
			return mock(SocketFactory.class);
		}

	}

	@Configuration
	static class FallbackMongoClientConfig {

		@Bean
		com.mongodb.client.MongoClient fallbackMongoClient() {
			return MongoClients.create();
		}

	}

}
