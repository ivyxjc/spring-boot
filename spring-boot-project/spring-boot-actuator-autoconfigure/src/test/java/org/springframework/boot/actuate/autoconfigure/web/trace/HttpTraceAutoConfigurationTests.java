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

package org.springframework.boot.actuate.autoconfigure.web.trace;

import org.junit.Test;
import org.springframework.boot.actuate.autoconfigure.trace.http.HttpTraceAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.trace.http.HttpTraceProperties;
import org.springframework.boot.actuate.trace.http.*;
import org.springframework.boot.actuate.web.trace.reactive.HttpTraceWebFilter;
import org.springframework.boot.actuate.web.trace.servlet.HttpTraceFilter;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ReactiveWebApplicationContextRunner;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link HttpTraceAutoConfiguration}.
 *
 * @author Andy Wilkinson
 */
public class HttpTraceAutoConfigurationTests {

	@Test
	public void configuresRepository() {
		new WebApplicationContextRunner().withConfiguration(AutoConfigurations.of(HttpTraceAutoConfiguration.class))
				.run((context) -> assertThat(context).hasSingleBean(InMemoryHttpTraceRepository.class));
	}

	@Test
	public void usesUserProvidedRepository() {
		new WebApplicationContextRunner().withConfiguration(AutoConfigurations.of(HttpTraceAutoConfiguration.class))
				.withUserConfiguration(CustomRepositoryConfiguration.class).run((context) -> {
			assertThat(context).hasSingleBean(HttpTraceRepository.class);
			assertThat(context.getBean(HttpTraceRepository.class))
					.isInstanceOf(CustomHttpTraceRepository.class);
		});
	}

	@Test
	public void configuresTracer() {
		new WebApplicationContextRunner().withConfiguration(AutoConfigurations.of(HttpTraceAutoConfiguration.class))
				.run((context) -> assertThat(context).hasSingleBean(HttpExchangeTracer.class));
	}

	@Test
	public void usesUserProvidedTracer() {
		new WebApplicationContextRunner().withConfiguration(AutoConfigurations.of(HttpTraceAutoConfiguration.class))
				.withUserConfiguration(CustomTracerConfiguration.class).run((context) -> {
			assertThat(context).hasSingleBean(HttpExchangeTracer.class);
			assertThat(context.getBean(HttpExchangeTracer.class)).isInstanceOf(CustomHttpExchangeTracer.class);
		});
	}

	@Test
	public void configuresWebFilter() {
		new ReactiveWebApplicationContextRunner()
				.withConfiguration(AutoConfigurations.of(HttpTraceAutoConfiguration.class))
				.run((context) -> assertThat(context).hasSingleBean(HttpTraceWebFilter.class));
	}

	@Test
	public void usesUserProvidedWebFilter() {
		new ReactiveWebApplicationContextRunner()
				.withConfiguration(AutoConfigurations.of(HttpTraceAutoConfiguration.class))
				.withUserConfiguration(CustomWebFilterConfiguration.class).run((context) -> {
			assertThat(context).hasSingleBean(HttpTraceWebFilter.class);
			assertThat(context.getBean(HttpTraceWebFilter.class)).isInstanceOf(CustomHttpTraceWebFilter.class);
		});
	}

	@Test
	public void configuresServletFilter() {
		new WebApplicationContextRunner().withConfiguration(AutoConfigurations.of(HttpTraceAutoConfiguration.class))
				.run((context) -> assertThat(context).hasSingleBean(HttpTraceFilter.class));
	}

	@Test
	public void usesUserProvidedServletFilter() {
		new WebApplicationContextRunner().withConfiguration(AutoConfigurations.of(HttpTraceAutoConfiguration.class))
				.withUserConfiguration(CustomFilterConfiguration.class).run((context) -> {
			assertThat(context).hasSingleBean(HttpTraceFilter.class);
			assertThat(context.getBean(HttpTraceFilter.class)).isInstanceOf(CustomHttpTraceFilter.class);
		});
	}

	@Test
	public void backsOffWhenDisabled() {
		new WebApplicationContextRunner().withConfiguration(AutoConfigurations.of(HttpTraceAutoConfiguration.class))
				.withPropertyValues("management.trace.http.enabled=false")
				.run((context) -> assertThat(context).doesNotHaveBean(InMemoryHttpTraceRepository.class)
						.doesNotHaveBean(HttpExchangeTracer.class).doesNotHaveBean(HttpTraceFilter.class));
	}

	private static class CustomHttpTraceRepository implements HttpTraceRepository {

		@Override
		public List<HttpTrace> findAll() {
			return null;
		}

		@Override
		public void add(HttpTrace trace) {

		}

	}

	@Configuration
	static class CustomRepositoryConfiguration {

		@Bean
		public CustomHttpTraceRepository customRepository() {
			return new CustomHttpTraceRepository();
		}

	}

	private static final class CustomHttpExchangeTracer extends HttpExchangeTracer {

		private CustomHttpExchangeTracer(Set<Include> includes) {
			super(includes);
		}

	}

	@Configuration
	static class CustomTracerConfiguration {

		@Bean
		public CustomHttpExchangeTracer customTracer(HttpTraceProperties properties) {
			return new CustomHttpExchangeTracer(properties.getInclude());
		}

	}

	private static final class CustomHttpTraceWebFilter extends HttpTraceWebFilter {

		private CustomHttpTraceWebFilter(HttpTraceRepository repository, HttpExchangeTracer tracer,
										 Set<Include> includes) {
			super(repository, tracer, includes);
		}

	}

	@Configuration
	static class CustomWebFilterConfiguration {

		@Bean
		public CustomHttpTraceWebFilter customWebFilter(HttpTraceRepository repository, HttpExchangeTracer tracer,
														HttpTraceProperties properties) {
			return new CustomHttpTraceWebFilter(repository, tracer, properties.getInclude());
		}

	}

	private static final class CustomHttpTraceFilter extends HttpTraceFilter {

		private CustomHttpTraceFilter(HttpTraceRepository repository, HttpExchangeTracer tracer) {
			super(repository, tracer);
		}

	}

	@Configuration
	static class CustomFilterConfiguration {

		@Bean
		public CustomHttpTraceFilter customWebFilter(HttpTraceRepository repository, HttpExchangeTracer tracer) {
			return new CustomHttpTraceFilter(repository, tracer);
		}

	}

}
