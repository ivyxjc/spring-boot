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

package org.springframework.boot.test.context.runner;

import org.springframework.boot.context.annotation.Configurations;
import org.springframework.boot.test.context.assertj.AssertableApplicationContext;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.util.List;
import java.util.function.Supplier;

/**
 * An {@link AbstractApplicationContextRunner ApplicationContext runner} for a standard,
 * non-web environment {@link ConfigurableApplicationContext}.
 * <p>
 * See {@link AbstractApplicationContextRunner} for details.
 *
 * @author Stephane Nicoll
 * @author Andy Wilkinson
 * @author Phillip Webb
 * @since 2.0.0
 */
public class ApplicationContextRunner extends
		AbstractApplicationContextRunner<ApplicationContextRunner, ConfigurableApplicationContext, AssertableApplicationContext> {

	/**
	 * Create a new {@link ApplicationContextRunner} instance using an
	 * {@link AnnotationConfigApplicationContext} as the underlying source.
	 */
	public ApplicationContextRunner() {
		this(AnnotationConfigApplicationContext::new);
	}

	/**
	 * Create a new {@link ApplicationContextRunner} instance using the specified
	 * {@code contextFactory} as the underlying source.
	 *
	 * @param contextFactory a supplier that returns a new instance on each call
	 */
	public ApplicationContextRunner(Supplier<ConfigurableApplicationContext> contextFactory) {
		super(contextFactory);
	}

	private ApplicationContextRunner(Supplier<ConfigurableApplicationContext> contextFactory,
									 List<ApplicationContextInitializer<? super ConfigurableApplicationContext>> initializers,
									 TestPropertyValues environmentProperties, TestPropertyValues systemProperties, ClassLoader classLoader,
									 ApplicationContext parent, List<Configurations> configurations) {
		super(contextFactory, initializers, environmentProperties, systemProperties, classLoader, parent,
				configurations);
	}

	@Override
	protected ApplicationContextRunner newInstance(Supplier<ConfigurableApplicationContext> contextFactory,
												   List<ApplicationContextInitializer<? super ConfigurableApplicationContext>> initializers,
												   TestPropertyValues environmentProperties, TestPropertyValues systemProperties, ClassLoader classLoader,
												   ApplicationContext parent, List<Configurations> configurations) {
		return new ApplicationContextRunner(contextFactory, initializers, environmentProperties, systemProperties,
				classLoader, parent, configurations);
	}

}
