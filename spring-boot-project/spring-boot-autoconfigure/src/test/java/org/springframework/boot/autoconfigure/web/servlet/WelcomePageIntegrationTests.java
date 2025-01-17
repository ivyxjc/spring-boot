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

package org.springframework.boot.autoconfigure.web.servlet;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.autoconfigure.http.HttpMessageConvertersAutoConfiguration;
import org.springframework.boot.autoconfigure.thymeleaf.ThymeleafAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit4.SpringRunner;

import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for the welcome page.
 *
 * @author Madhura Bhave
 */
@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
				properties = {"spring.resources.chain.strategy.content.enabled=true",
						"spring.thymeleaf.prefix=classpath:/templates/thymeleaf/"})
public class WelcomePageIntegrationTests {

	@LocalServerPort
	private int port;

	private TestRestTemplate template = new TestRestTemplate();

	@Test
	public void contentStrategyWithWelcomePage() throws Exception {
		RequestEntity<?> entity = RequestEntity.get(new URI("http://localhost:" + this.port + "/"))
				.header("Accept", MediaType.ALL.toString()).build();
		ResponseEntity<String> content = this.template.exchange(entity, String.class);
		assertThat(content.getBody()).contains("/custom-");
	}

	@Configuration
	@Import({PropertyPlaceholderAutoConfiguration.class, WebMvcAutoConfiguration.class,
					HttpMessageConvertersAutoConfiguration.class, ServletWebServerFactoryAutoConfiguration.class,
					DispatcherServletAutoConfiguration.class, ThymeleafAutoConfiguration.class})
	public static class TestConfiguration {

		public static void main(String[] args) {
			new SpringApplicationBuilder(TestConfiguration.class).run(args);
		}

	}

}
