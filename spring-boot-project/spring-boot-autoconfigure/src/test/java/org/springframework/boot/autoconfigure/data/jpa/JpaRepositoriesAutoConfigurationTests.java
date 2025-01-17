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

package org.springframework.boot.autoconfigure.data.jpa;

import org.junit.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.TestAutoConfigurationPackage;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.autoconfigure.data.alt.jpa.CityJpaRepository;
import org.springframework.boot.autoconfigure.data.alt.mongo.CityMongoDbRepository;
import org.springframework.boot.autoconfigure.data.alt.solr.CitySolrRepository;
import org.springframework.boot.autoconfigure.data.jpa.city.City;
import org.springframework.boot.autoconfigure.data.jpa.city.CityRepository;
import org.springframework.boot.autoconfigure.jdbc.EmbeddedDataSourceConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.autoconfigure.task.TaskExecutionAutoConfiguration;
import org.springframework.boot.autoconfigure.task.TaskSchedulingAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan.Filter;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.PlatformTransactionManager;

import javax.persistence.EntityManagerFactory;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link JpaRepositoriesAutoConfiguration}.
 *
 * @author Dave Syer
 * @author Oliver Gierke
 */
public class JpaRepositoriesAutoConfigurationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(HibernateJpaAutoConfiguration.class,
					JpaRepositoriesAutoConfiguration.class, PropertyPlaceholderAutoConfiguration.class))
			.withUserConfiguration(EmbeddedDataSourceConfiguration.class);

	@Test
	public void testDefaultRepositoryConfiguration() {
		this.contextRunner.withUserConfiguration(TestConfiguration.class).run((context) -> {
			assertThat(context).hasSingleBean(CityRepository.class);
			assertThat(context).hasSingleBean(PlatformTransactionManager.class);
			assertThat(context).hasSingleBean(EntityManagerFactory.class);
			assertThat(context.getBean(LocalContainerEntityManagerFactoryBean.class).getBootstrapExecutor()).isNull();
		});
	}

	@Test
	public void testOverrideRepositoryConfiguration() {
		this.contextRunner.withUserConfiguration(CustomConfiguration.class).run((context) -> {
			assertThat(context).hasSingleBean(CityJpaRepository.class);
			assertThat(context).hasSingleBean(PlatformTransactionManager.class);
			assertThat(context).hasSingleBean(EntityManagerFactory.class);
		});
	}

	@Test
	public void autoConfigurationShouldNotKickInEvenIfManualConfigDidNotCreateAnyRepositories() {
		this.contextRunner.withUserConfiguration(SortOfInvalidCustomConfiguration.class)
				.run((context) -> assertThat(context).doesNotHaveBean(CityRepository.class));
	}

	@Test
	public void whenBootstrappingModeIsLazyWithMultipleAsyncExecutorBootstrapExecutorIsConfigured() {
		this.contextRunner.withUserConfiguration(MultipleAsyncTaskExecutorConfiguration.class)
				.withConfiguration(AutoConfigurations.of(TaskExecutionAutoConfiguration.class,
						TaskSchedulingAutoConfiguration.class))
				.withPropertyValues("spring.data.jpa.repositories.bootstrap-mode=lazy")
				.run((context) -> assertThat(
						context.getBean(LocalContainerEntityManagerFactoryBean.class).getBootstrapExecutor())
						.isEqualTo(context.getBean("applicationTaskExecutor")));
	}

	@Test
	public void whenBootstrappingModeIsLazyWithSingleAsyncExecutorBootstrapExecutorIsConfigured() {
		this.contextRunner.withUserConfiguration(SingleAsyncTaskExecutorConfiguration.class)
				.withPropertyValues("spring.data.jpa.repositories.bootstrap-mode=lazy")
				.run((context) -> assertThat(
						context.getBean(LocalContainerEntityManagerFactoryBean.class).getBootstrapExecutor())
						.isEqualTo(context.getBean("testAsyncTaskExecutor")));
	}

	@Test
	public void whenBootstrappingModeIsDeferredBootstrapExecutorIsConfigured() {
		this.contextRunner.withUserConfiguration(MultipleAsyncTaskExecutorConfiguration.class)
				.withConfiguration(AutoConfigurations.of(TaskExecutionAutoConfiguration.class,
						TaskSchedulingAutoConfiguration.class))
				.withPropertyValues("spring.data.jpa.repositories.bootstrap-mode=deferred")
				.run((context) -> assertThat(
						context.getBean(LocalContainerEntityManagerFactoryBean.class).getBootstrapExecutor())
						.isEqualTo(context.getBean("applicationTaskExecutor")));
	}

	@Test
	public void whenBootstrappingModeIsDefaultBootstrapExecutorIsNotConfigured() {
		this.contextRunner.withUserConfiguration(MultipleAsyncTaskExecutorConfiguration.class)
				.withConfiguration(AutoConfigurations.of(TaskExecutionAutoConfiguration.class,
						TaskSchedulingAutoConfiguration.class))
				.withPropertyValues("spring.data.jpa.repositories.bootstrap-mode=default").run((context) -> assertThat(
				context.getBean(LocalContainerEntityManagerFactoryBean.class).getBootstrapExecutor()).isNull());
	}

	@Configuration
	@EnableScheduling
	@Import(TestConfiguration.class)
	protected static class MultipleAsyncTaskExecutorConfiguration {

	}

	@Configuration
	@Import(TestConfiguration.class)
	protected static class SingleAsyncTaskExecutorConfiguration {

		@Bean
		public SimpleAsyncTaskExecutor testAsyncTaskExecutor() {
			return new SimpleAsyncTaskExecutor();
		}

	}

	@Configuration
	@TestAutoConfigurationPackage(City.class)
	protected static class TestConfiguration {

	}

	@Configuration
	@EnableJpaRepositories(
			basePackageClasses = org.springframework.boot.autoconfigure.data.alt.jpa.CityJpaRepository.class,
			excludeFilters = {@Filter(type = FilterType.ASSIGNABLE_TYPE, value = CityMongoDbRepository.class),
					@Filter(type = FilterType.ASSIGNABLE_TYPE, value = CitySolrRepository.class)})
	@TestAutoConfigurationPackage(City.class)
	protected static class CustomConfiguration {

	}

	@Configuration
	// To not find any repositories
	@EnableJpaRepositories("foo.bar")
	@TestAutoConfigurationPackage(City.class)
	protected static class SortOfInvalidCustomConfiguration {

	}

}
