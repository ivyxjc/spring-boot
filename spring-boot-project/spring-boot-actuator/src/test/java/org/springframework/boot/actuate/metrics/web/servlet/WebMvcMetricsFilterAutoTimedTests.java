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

package org.springframework.boot.actuate.metrics.web.servlet;

import io.micrometer.core.instrument.Clock;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.MockClock;
import io.micrometer.core.instrument.simple.SimpleConfig;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Test for {@link WebMvcMetricsFilter} with auto-timed enabled.
 *
 * @author Jon Schneider
 */
@RunWith(SpringRunner.class)
@WebAppConfiguration
public class WebMvcMetricsFilterAutoTimedTests {

	@Autowired
	private MeterRegistry registry;

	@Autowired
	private WebApplicationContext context;

	private MockMvc mvc;

	@Autowired
	private WebMvcMetricsFilter filter;

	@Before
	public void setupMockMvc() {
		this.mvc = MockMvcBuilders.webAppContextSetup(this.context).addFilters(this.filter).build();
	}

	@Test
	public void metricsCanBeAutoTimed() throws Exception {
		this.mvc.perform(get("/api/10")).andExpect(status().isOk());
		assertThat(this.registry.get("http.server.requests").tags("status", "200").timer().count()).isEqualTo(1L);
	}

	@Configuration
	@EnableWebMvc
	@Import({Controller.class})
	static class TestConfiguration {

		@Bean
		MockClock clock() {
			return new MockClock();
		}

		@Bean
		MeterRegistry meterRegistry(Clock clock) {
			return new SimpleMeterRegistry(SimpleConfig.DEFAULT, clock);
		}

		@Bean
		public WebMvcMetricsFilter webMetricsFilter(WebApplicationContext context, MeterRegistry registry) {
			return new WebMvcMetricsFilter(registry, new DefaultWebMvcTagsProvider(), "http.server.requests", true);
		}

	}

	@RestController
	@RequestMapping("/api")
	static class Controller {

		@GetMapping("/{id}")
		public String successful(@PathVariable Long id) {
			return id.toString();
		}

	}

}
