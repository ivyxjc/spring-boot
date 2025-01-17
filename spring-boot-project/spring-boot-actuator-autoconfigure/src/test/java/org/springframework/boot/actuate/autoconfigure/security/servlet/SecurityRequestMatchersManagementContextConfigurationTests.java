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

package org.springframework.boot.actuate.autoconfigure.security.servlet;

import org.junit.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.security.servlet.AntPathRequestMatcherProvider;
import org.springframework.boot.autoconfigure.security.servlet.RequestMatcherProvider;
import org.springframework.boot.autoconfigure.web.servlet.DispatcherServletPath;
import org.springframework.boot.autoconfigure.web.servlet.JerseyApplicationPath;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link SecurityRequestMatchersManagementContextConfiguration}.
 *
 * @author Madhura Bhave
 */
public class SecurityRequestMatchersManagementContextConfigurationTests {

	private WebApplicationContextRunner contextRunner = new WebApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(SecurityRequestMatchersManagementContextConfiguration.class));

	@Test
	public void configurationConditionalOnWebApplication() {
		new ApplicationContextRunner()
				.withConfiguration(AutoConfigurations.of(SecurityRequestMatchersManagementContextConfiguration.class))
				.withUserConfiguration(TestMvcConfiguration.class)
				.run((context) -> assertThat(context).doesNotHaveBean(RequestMatcherProvider.class));
	}

	@Test
	public void configurationConditionalOnRequestMatcherClass() {
		this.contextRunner
				.withClassLoader(
						new FilteredClassLoader("org.springframework.security.web.util.matcher.RequestMatcher"))
				.run((context) -> assertThat(context).doesNotHaveBean(RequestMatcherProvider.class));
	}

	@Test
	public void registersRequestMatcherProviderIfMvcPresent() {
		this.contextRunner.withUserConfiguration(TestMvcConfiguration.class).run((context) -> {
			AntPathRequestMatcherProvider matcherProvider = context.getBean(AntPathRequestMatcherProvider.class);
			RequestMatcher requestMatcher = matcherProvider.getRequestMatcher("/example");
			assertThat(ReflectionTestUtils.getField(requestMatcher, "pattern")).isEqualTo("/custom/example");
		});
	}

	@Test
	public void registersRequestMatcherForJerseyProviderIfJerseyPresentAndMvcAbsent() {
		this.contextRunner.withClassLoader(new FilteredClassLoader("org.springframework.web.servlet.DispatcherServlet"))
				.withUserConfiguration(TestJerseyConfiguration.class).run((context) -> {
			AntPathRequestMatcherProvider matcherProvider = context
					.getBean(AntPathRequestMatcherProvider.class);
			RequestMatcher requestMatcher = matcherProvider.getRequestMatcher("/example");
			assertThat(ReflectionTestUtils.getField(requestMatcher, "pattern")).isEqualTo("/admin/example");
		});
	}

	@Test
	public void mvcRequestMatcherProviderConditionalOnDispatcherServletClass() {
		this.contextRunner.withClassLoader(new FilteredClassLoader("org.springframework.web.servlet.DispatcherServlet"))
				.run((context) -> assertThat(context).doesNotHaveBean(AntPathRequestMatcherProvider.class));
	}

	@Test
	public void mvcRequestMatcherProviderConditionalOnDispatcherServletPathBean() {
		new WebApplicationContextRunner()
				.withConfiguration(AutoConfigurations.of(SecurityRequestMatchersManagementContextConfiguration.class))
				.run((context) -> assertThat(context).doesNotHaveBean(AntPathRequestMatcherProvider.class));
	}

	@Test
	public void jerseyRequestMatcherProviderConditionalOnResourceConfigClass() {
		this.contextRunner.withClassLoader(new FilteredClassLoader("org.glassfish.jersey.server.ResourceConfig"))
				.run((context) -> assertThat(context).doesNotHaveBean(AntPathRequestMatcherProvider.class));
	}

	@Test
	public void jerseyRequestMatcherProviderConditionalOnJerseyApplicationPathBean() {
		new WebApplicationContextRunner()
				.withConfiguration(AutoConfigurations.of(SecurityRequestMatchersManagementContextConfiguration.class))
				.withClassLoader(new FilteredClassLoader("org.springframework.web.servlet.DispatcherServlet"))
				.run((context) -> assertThat(context).doesNotHaveBean(AntPathRequestMatcherProvider.class));
	}

	@Configuration
	static class TestMvcConfiguration {

		@Bean
		public DispatcherServletPath dispatcherServletPath() {
			return () -> "/custom";
		}

	}

	@Configuration
	static class TestJerseyConfiguration {

		@Bean
		public JerseyApplicationPath jerseyApplicationPath() {
			return () -> "/admin";
		}

	}

}
