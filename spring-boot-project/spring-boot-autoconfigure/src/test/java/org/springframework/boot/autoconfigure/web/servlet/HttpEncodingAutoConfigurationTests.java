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

import org.junit.After;
import org.junit.Test;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.boot.web.server.WebServerFactoryCustomizerBeanPostProcessor;
import org.springframework.boot.web.servlet.filter.OrderedFormContentFilter;
import org.springframework.boot.web.servlet.filter.OrderedHiddenHttpMethodFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.mock.web.MockServletContext;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.filter.CharacterEncodingFilter;
import org.springframework.web.filter.HiddenHttpMethodFilter;

import javax.servlet.Filter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Tests for {@link HttpEncodingAutoConfiguration}
 *
 * @author Stephane Nicoll
 */
public class HttpEncodingAutoConfigurationTests {

	private AnnotationConfigWebApplicationContext context;

	@After
	public void close() {
		if (this.context != null) {
			this.context.close();
		}
	}

	@Test
	public void defaultConfiguration() {
		load(EmptyConfiguration.class);
		CharacterEncodingFilter filter = this.context.getBean(CharacterEncodingFilter.class);
		assertCharacterEncodingFilter(filter, "UTF-8", true, false);
	}

	@Test
	public void disableConfiguration() {
		load(EmptyConfiguration.class, "spring.http.encoding.enabled:false");
		assertThatExceptionOfType(NoSuchBeanDefinitionException.class)
				.isThrownBy(() -> this.context.getBean(CharacterEncodingFilter.class));
	}

	@Test
	public void customConfiguration() {
		load(EmptyConfiguration.class, "spring.http.encoding.charset:ISO-8859-15", "spring.http.encoding.force:false");
		CharacterEncodingFilter filter = this.context.getBean(CharacterEncodingFilter.class);
		assertCharacterEncodingFilter(filter, "ISO-8859-15", false, false);
	}

	@Test
	public void customFilterConfiguration() {
		load(FilterConfiguration.class, "spring.http.encoding.charset:ISO-8859-15", "spring.http.encoding.force:false");
		CharacterEncodingFilter filter = this.context.getBean(CharacterEncodingFilter.class);
		assertCharacterEncodingFilter(filter, "US-ASCII", false, false);
	}

	@Test
	public void forceRequest() {
		load(EmptyConfiguration.class, "spring.http.encoding.force-request:false");
		CharacterEncodingFilter filter = this.context.getBean(CharacterEncodingFilter.class);
		assertCharacterEncodingFilter(filter, "UTF-8", false, false);
	}

	@Test
	public void forceResponse() {
		load(EmptyConfiguration.class, "spring.http.encoding.force-response:true");
		CharacterEncodingFilter filter = this.context.getBean(CharacterEncodingFilter.class);
		assertCharacterEncodingFilter(filter, "UTF-8", true, true);
	}

	@Test
	public void forceRequestOverridesForce() {
		load(EmptyConfiguration.class, "spring.http.encoding.force:true", "spring.http.encoding.force-request:false");
		CharacterEncodingFilter filter = this.context.getBean(CharacterEncodingFilter.class);
		assertCharacterEncodingFilter(filter, "UTF-8", false, true);
	}

	@Test
	public void forceResponseOverridesForce() {
		load(EmptyConfiguration.class, "spring.http.encoding.force:true", "spring.http.encoding.force-response:false");
		CharacterEncodingFilter filter = this.context.getBean(CharacterEncodingFilter.class);
		assertCharacterEncodingFilter(filter, "UTF-8", true, false);
	}

	@Test
	public void filterIsOrderedHighest() {
		load(OrderedConfiguration.class);
		List<Filter> beans = new ArrayList<>(this.context.getBeansOfType(Filter.class).values());
		AnnotationAwareOrderComparator.sort(beans);
		assertThat(beans.get(0)).isInstanceOf(CharacterEncodingFilter.class);
		assertThat(beans.get(1)).isInstanceOf(HiddenHttpMethodFilter.class);
	}

	@Test
	public void noLocaleCharsetMapping() {
		load(EmptyConfiguration.class);
		Map<String, WebServerFactoryCustomizer<?>> beans = getWebServerFactoryCustomizerBeans();
		assertThat(beans.size()).isEqualTo(1);
		assertThat(this.context.getBean(MockServletWebServerFactory.class).getLocaleCharsetMappings()).isEmpty();
	}

	@Test
	public void customLocaleCharsetMappings() {
		load(EmptyConfiguration.class, "spring.http.encoding.mapping.en:UTF-8",
				"spring.http.encoding.mapping.fr_FR:UTF-8");
		Map<String, WebServerFactoryCustomizer<?>> beans = getWebServerFactoryCustomizerBeans();
		assertThat(beans.size()).isEqualTo(1);
		assertThat(this.context.getBean(MockServletWebServerFactory.class).getLocaleCharsetMappings().size())
				.isEqualTo(2);
		assertThat(
				this.context.getBean(MockServletWebServerFactory.class).getLocaleCharsetMappings().get(Locale.ENGLISH))
				.isEqualTo(StandardCharsets.UTF_8);
		assertThat(
				this.context.getBean(MockServletWebServerFactory.class).getLocaleCharsetMappings().get(Locale.FRANCE))
				.isEqualTo(StandardCharsets.UTF_8);
	}

	@SuppressWarnings({"unchecked", "rawtypes"})
	private Map<String, WebServerFactoryCustomizer<?>> getWebServerFactoryCustomizerBeans() {
		return (Map) this.context.getBeansOfType(WebServerFactoryCustomizer.class);
	}

	private void assertCharacterEncodingFilter(CharacterEncodingFilter actual, String encoding,
											   boolean forceRequestEncoding, boolean forceResponseEncoding) {
		assertThat(actual.getEncoding()).isEqualTo(encoding);
		assertThat(actual.isForceRequestEncoding()).isEqualTo(forceRequestEncoding);
		assertThat(actual.isForceResponseEncoding()).isEqualTo(forceResponseEncoding);
	}

	private void load(Class<?> config, String... environment) {
		this.context = doLoad(new Class<?>[]{config}, environment);
	}

	private AnnotationConfigWebApplicationContext doLoad(Class<?>[] configs, String... environment) {
		AnnotationConfigWebApplicationContext applicationContext = new AnnotationConfigWebApplicationContext();
		TestPropertyValues.of(environment).applyTo(applicationContext);
		applicationContext.register(configs);
		applicationContext.register(MinimalWebAutoConfiguration.class, HttpEncodingAutoConfiguration.class);
		applicationContext.setServletContext(new MockServletContext());
		applicationContext.refresh();
		return applicationContext;
	}

	@Configuration
	static class EmptyConfiguration {

	}

	@Configuration
	static class FilterConfiguration {

		@Bean
		public CharacterEncodingFilter myCharacterEncodingFilter() {
			CharacterEncodingFilter filter = new CharacterEncodingFilter();
			filter.setEncoding("US-ASCII");
			filter.setForceEncoding(false);
			return filter;
		}

	}

	@Configuration
	static class OrderedConfiguration {

		@Bean
		public OrderedHiddenHttpMethodFilter hiddenHttpMethodFilter() {
			return new OrderedHiddenHttpMethodFilter();
		}

		@Bean
		public OrderedFormContentFilter formContentFilter() {
			return new OrderedFormContentFilter();
		}

	}

	@Configuration
	static class MinimalWebAutoConfiguration {

		@Bean
		public MockServletWebServerFactory MockServletWebServerFactory() {
			return new MockServletWebServerFactory();
		}

		@Bean
		public WebServerFactoryCustomizerBeanPostProcessor ServletWebServerCustomizerBeanPostProcessor() {
			return new WebServerFactoryCustomizerBeanPostProcessor();
		}

	}

}
