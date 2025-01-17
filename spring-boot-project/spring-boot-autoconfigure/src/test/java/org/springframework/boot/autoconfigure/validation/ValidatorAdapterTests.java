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

package org.springframework.boot.autoconfigure.validation;

import org.junit.Test;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.validation.MapBindingResult;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import javax.validation.constraints.Min;
import java.util.HashMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Tests for {@link ValidatorAdapter}.
 *
 * @author Stephane Nicoll
 * @author Madhura Bhave
 */
public class ValidatorAdapterTests {

	private ApplicationContextRunner contextRunner = new ApplicationContextRunner();

	@Test
	public void wrapLocalValidatorFactoryBean() {
		this.contextRunner.withUserConfiguration(LocalValidatorFactoryBeanConfig.class).run((context) -> {
			ValidatorAdapter wrapper = context.getBean(ValidatorAdapter.class);
			assertThat(wrapper.supports(SampleData.class)).isTrue();
			MapBindingResult errors = new MapBindingResult(new HashMap<String, Object>(), "test");
			wrapper.validate(new SampleData(40), errors);
			assertThat(errors.getErrorCount()).isEqualTo(1);
		});
	}

	@Test
	public void wrapperInvokesCallbackOnNonManagedBean() {
		this.contextRunner.withUserConfiguration(NonManagedBeanConfig.class).run((context) -> {
			LocalValidatorFactoryBean validator = context.getBean(NonManagedBeanConfig.class).validator;
			verify(validator, times(1)).setApplicationContext(any(ApplicationContext.class));
			verify(validator, times(1)).afterPropertiesSet();
			verify(validator, never()).destroy();
			context.close();
			verify(validator, times(1)).destroy();
		});
	}

	@Test
	public void wrapperDoesNotInvokeCallbackOnManagedBean() {
		this.contextRunner.withUserConfiguration(ManagedBeanConfig.class).run((context) -> {
			LocalValidatorFactoryBean validator = context.getBean(ManagedBeanConfig.class).validator;
			verify(validator, never()).setApplicationContext(any(ApplicationContext.class));
			verify(validator, never()).afterPropertiesSet();
			verify(validator, never()).destroy();
			context.close();
			verify(validator, never()).destroy();
		});
	}

	@Test
	public void wrapperWhenValidationProviderNotPresentShouldNotThrowException() {
		ClassPathResource hibernateValidator = new ClassPathResource(
				"META-INF/services/javax.validation.spi.ValidationProvider");
		this.contextRunner
				.withClassLoader(
						new FilteredClassLoader(FilteredClassLoader.ClassPathResourceFilter.of(hibernateValidator),
								FilteredClassLoader.PackageFilter.of("org.hibernate.validator")))
				.run((context) -> ValidatorAdapter.get(context, null));
	}

	@Configuration
	static class LocalValidatorFactoryBeanConfig {

		@Bean
		public LocalValidatorFactoryBean validator() {
			return new LocalValidatorFactoryBean();
		}

		@Bean
		public ValidatorAdapter wrapper() {
			return new ValidatorAdapter(validator(), true);
		}

	}

	@Configuration
	static class NonManagedBeanConfig {

		private final LocalValidatorFactoryBean validator = mock(LocalValidatorFactoryBean.class);

		@Bean
		public ValidatorAdapter wrapper() {
			return new ValidatorAdapter(this.validator, false);
		}

	}

	@Configuration
	static class ManagedBeanConfig {

		private final LocalValidatorFactoryBean validator = mock(LocalValidatorFactoryBean.class);

		@Bean
		public ValidatorAdapter wrapper() {
			return new ValidatorAdapter(this.validator, true);
		}

	}

	static class SampleData {

		@Min(42)
		private int counter;

		SampleData(int counter) {
			this.counter = counter;
		}

	}

}
