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

package org.springframework.boot.web.servlet;

import org.junit.After;
import org.junit.Test;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.AnnotationConfigurationException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Tests for {@link ServletComponentScanRegistrar}
 *
 * @author Andy Wilkinson
 */
public class ServletComponentScanRegistrarTests {

	private AnnotationConfigApplicationContext context;

	@After
	public void after() {
		if (this.context != null) {
			this.context.close();
		}
	}

	@Test
	public void packagesConfiguredWithValue() {
		this.context = new AnnotationConfigApplicationContext(ValuePackages.class);
		ServletComponentRegisteringPostProcessor postProcessor = this.context
				.getBean(ServletComponentRegisteringPostProcessor.class);
		assertThat(postProcessor.getPackagesToScan()).contains("com.example.foo", "com.example.bar");
	}

	@Test
	public void packagesConfiguredWithValueAsm() {
		this.context = new AnnotationConfigApplicationContext();
		this.context.registerBeanDefinition("valuePackages", new RootBeanDefinition(ValuePackages.class.getName()));
		this.context.refresh();
		ServletComponentRegisteringPostProcessor postProcessor = this.context
				.getBean(ServletComponentRegisteringPostProcessor.class);
		assertThat(postProcessor.getPackagesToScan()).contains("com.example.foo", "com.example.bar");
	}

	@Test
	public void packagesConfiguredWithBackPackages() {
		this.context = new AnnotationConfigApplicationContext(BasePackages.class);
		ServletComponentRegisteringPostProcessor postProcessor = this.context
				.getBean(ServletComponentRegisteringPostProcessor.class);
		assertThat(postProcessor.getPackagesToScan()).contains("com.example.foo", "com.example.bar");
	}

	@Test
	public void packagesConfiguredWithBasePackageClasses() {
		this.context = new AnnotationConfigApplicationContext(BasePackageClasses.class);
		ServletComponentRegisteringPostProcessor postProcessor = this.context
				.getBean(ServletComponentRegisteringPostProcessor.class);
		assertThat(postProcessor.getPackagesToScan()).contains(getClass().getPackage().getName());
	}

	@Test
	public void packagesConfiguredWithBothValueAndBasePackages() {
		assertThatExceptionOfType(AnnotationConfigurationException.class)
				.isThrownBy(() -> this.context = new AnnotationConfigApplicationContext(ValueAndBasePackages.class))
				.withMessageContaining("'value'").withMessageContaining("'basePackages'")
				.withMessageContaining("com.example.foo").withMessageContaining("com.example.bar");
	}

	@Test
	public void packagesFromMultipleAnnotationsAreMerged() {
		this.context = new AnnotationConfigApplicationContext(BasePackages.class, AdditionalPackages.class);
		ServletComponentRegisteringPostProcessor postProcessor = this.context
				.getBean(ServletComponentRegisteringPostProcessor.class);
		assertThat(postProcessor.getPackagesToScan()).contains("com.example.foo", "com.example.bar", "com.example.baz");
	}

	@Test
	public void withNoBasePackagesScanningUsesBasePackageOfAnnotatedClass() {
		this.context = new AnnotationConfigApplicationContext(NoBasePackages.class);
		ServletComponentRegisteringPostProcessor postProcessor = this.context
				.getBean(ServletComponentRegisteringPostProcessor.class);
		assertThat(postProcessor.getPackagesToScan()).containsExactly("org.springframework.boot.web.servlet");
	}

	@Test
	public void noBasePackageAndBasePackageAreCombinedCorrectly() {
		this.context = new AnnotationConfigApplicationContext(NoBasePackages.class, BasePackages.class);
		ServletComponentRegisteringPostProcessor postProcessor = this.context
				.getBean(ServletComponentRegisteringPostProcessor.class);
		assertThat(postProcessor.getPackagesToScan()).containsExactlyInAnyOrder("org.springframework.boot.web.servlet",
				"com.example.foo", "com.example.bar");
	}

	@Test
	public void basePackageAndNoBasePackageAreCombinedCorrectly() {
		this.context = new AnnotationConfigApplicationContext(BasePackages.class, NoBasePackages.class);
		ServletComponentRegisteringPostProcessor postProcessor = this.context
				.getBean(ServletComponentRegisteringPostProcessor.class);
		assertThat(postProcessor.getPackagesToScan()).containsExactlyInAnyOrder("org.springframework.boot.web.servlet",
				"com.example.foo", "com.example.bar");
	}

	@Configuration
	@ServletComponentScan({"com.example.foo", "com.example.bar"})
	static class ValuePackages {

	}

	@Configuration
	@ServletComponentScan(basePackages = {"com.example.foo", "com.example.bar"})
	static class BasePackages {

	}

	@Configuration
	@ServletComponentScan(basePackages = "com.example.baz")
	static class AdditionalPackages {

	}

	@Configuration
	@ServletComponentScan(basePackageClasses = ServletComponentScanRegistrarTests.class)
	static class BasePackageClasses {

	}

	@Configuration
	@ServletComponentScan(value = "com.example.foo", basePackages = "com.example.bar")
	static class ValueAndBasePackages {

	}

	@Configuration
	@ServletComponentScan
	static class NoBasePackages {

	}

}
