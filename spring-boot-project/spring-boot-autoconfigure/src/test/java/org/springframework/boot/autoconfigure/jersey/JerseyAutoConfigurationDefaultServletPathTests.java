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

package org.springframework.boot.autoconfigure.jersey;

import org.glassfish.jersey.server.ResourceConfig;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.ServletWebServerFactoryAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import java.lang.annotation.*;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link JerseyAutoConfiguration} when using default servlet paths.
 *
 * @author Dave Syer
 */

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@DirtiesContext
public class JerseyAutoConfigurationDefaultServletPathTests {

	@Autowired
	private TestRestTemplate restTemplate;

	@Test
	public void contextLoads() {
		ResponseEntity<String> entity = this.restTemplate.getForEntity("/hello", String.class);
		assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.OK);
	}

	@Target(ElementType.TYPE)
	@Retention(RetentionPolicy.RUNTIME)
	@Documented
	@Configuration
	@Import({ServletWebServerFactoryAutoConfiguration.class, JerseyAutoConfiguration.class,
					PropertyPlaceholderAutoConfiguration.class})
	protected @interface MinimalWebConfiguration {

	}

	@MinimalWebConfiguration
	@Path("/hello")
	public static class Application extends ResourceConfig {

		@Value("${message:World}")
		private String msg;

		public Application() {
			register(Application.class);
		}

		public static void main(String[] args) {
			SpringApplication.run(Application.class, args);
		}

		@GET
		public String message() {
			return "Hello " + this.msg;
		}

	}

}
