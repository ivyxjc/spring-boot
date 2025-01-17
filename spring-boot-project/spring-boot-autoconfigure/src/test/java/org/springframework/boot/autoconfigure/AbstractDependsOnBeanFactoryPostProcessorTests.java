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

package org.springframework.boot.autoconfigure;

import org.junit.Test;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.test.context.assertj.AssertableApplicationContext;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link AbstractDependsOnBeanFactoryPostProcessor}.
 *
 * @author Dmytro Nosan
 */
public class AbstractDependsOnBeanFactoryPostProcessorTests {

	private ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withUserConfiguration(FooBarConfiguration.class);

	@Test
	public void fooBeansShouldDependOnBarBeanNames() {
		this.contextRunner
				.withUserConfiguration(FooDependsOnBarNamePostProcessor.class, FooBarFactoryBeanConfiguration.class)
				.run(this::assertThatFooDependsOnBar);
	}

	@Test
	public void fooBeansShouldDependOnBarBeanTypes() {
		this.contextRunner
				.withUserConfiguration(FooDependsOnBarTypePostProcessor.class, FooBarFactoryBeanConfiguration.class)
				.run(this::assertThatFooDependsOnBar);
	}

	@Test
	public void fooBeansShouldDependOnBarBeanNamesParentContext() {
		try (AnnotationConfigApplicationContext parentContext = new AnnotationConfigApplicationContext(
				FooBarFactoryBeanConfiguration.class)) {
			this.contextRunner.withUserConfiguration(FooDependsOnBarNamePostProcessor.class).withParent(parentContext)
					.run(this::assertThatFooDependsOnBar);
		}
	}

	@Test
	public void fooBeansShouldDependOnBarBeanTypesParentContext() {
		try (AnnotationConfigApplicationContext parentContext = new AnnotationConfigApplicationContext(
				FooBarFactoryBeanConfiguration.class)) {
			this.contextRunner.withUserConfiguration(FooDependsOnBarTypePostProcessor.class).withParent(parentContext)
					.run(this::assertThatFooDependsOnBar);
		}
	}

	private void assertThatFooDependsOnBar(AssertableApplicationContext context) {
		ConfigurableListableBeanFactory beanFactory = context.getBeanFactory();
		assertThat(getBeanDefinition("foo", beanFactory).getDependsOn()).containsExactly("bar", "barFactoryBean");
		assertThat(getBeanDefinition("fooFactoryBean", beanFactory).getDependsOn()).containsExactly("bar",
				"barFactoryBean");
	}

	private BeanDefinition getBeanDefinition(String beanName, ConfigurableListableBeanFactory beanFactory) {
		try {
			return beanFactory.getBeanDefinition(beanName);
		} catch (NoSuchBeanDefinitionException ex) {
			BeanFactory parentBeanFactory = beanFactory.getParentBeanFactory();
			if (parentBeanFactory instanceof ConfigurableListableBeanFactory) {
				return getBeanDefinition(beanName, (ConfigurableListableBeanFactory) parentBeanFactory);
			}
			throw ex;
		}
	}

	static class Foo {

	}

	static class Bar {

	}

	@Configuration
	static class FooBarFactoryBeanConfiguration {

		@Bean
		public FooFactoryBean fooFactoryBean() {
			return new FooFactoryBean();
		}

		@Bean
		public BarFactoryBean barFactoryBean() {
			return new BarFactoryBean();
		}

	}

	@Configuration
	static class FooBarConfiguration {

		@Bean
		public Bar bar() {
			return new Bar();
		}

		@Bean
		public Foo foo() {
			return new Foo();
		}

	}

	static class FooDependsOnBarTypePostProcessor extends AbstractDependsOnBeanFactoryPostProcessor {

		protected FooDependsOnBarTypePostProcessor() {
			super(Foo.class, FooFactoryBean.class, Bar.class, BarFactoryBean.class);
		}

	}

	static class FooDependsOnBarNamePostProcessor extends AbstractDependsOnBeanFactoryPostProcessor {

		protected FooDependsOnBarNamePostProcessor() {
			super(Foo.class, FooFactoryBean.class, "bar", "barFactoryBean");
		}

	}

	static class FooFactoryBean implements FactoryBean<Foo> {

		@Override
		public Foo getObject() {
			return new Foo();
		}

		@Override
		public Class<?> getObjectType() {
			return Foo.class;
		}

	}

	static class BarFactoryBean implements FactoryBean<Bar> {

		@Override
		public Bar getObject() {
			return new Bar();
		}

		@Override
		public Class<?> getObjectType() {
			return Bar.class;
		}

	}

}
