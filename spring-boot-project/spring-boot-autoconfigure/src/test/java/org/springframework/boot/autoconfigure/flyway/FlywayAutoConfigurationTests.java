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

package org.springframework.boot.autoconfigure.flyway;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.Location;
import org.flywaydb.core.api.MigrationVersion;
import org.flywaydb.core.api.callback.Callback;
import org.flywaydb.core.api.callback.Context;
import org.flywaydb.core.api.callback.Event;
import org.flywaydb.core.api.callback.FlywayCallback;
import org.hibernate.engine.transaction.jta.platform.internal.NoJtaPlatform;
import org.junit.Test;
import org.mockito.InOrder;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.jdbc.EmbeddedDataSourceConfiguration;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.boot.jdbc.SchemaManagement;
import org.springframework.boot.orm.jpa.EntityManagerFactoryBuilder;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcOperations;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link FlywayAutoConfiguration}.
 *
 * @author Dave Syer
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @author Vedran Pavic
 * @author Eddú Meléndez
 * @author Stephane Nicoll
 * @author Dominic Gunn
 * @author András Deák
 */
@SuppressWarnings("deprecation")
public class FlywayAutoConfigurationTests {

	private ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(FlywayAutoConfiguration.class))
			.withPropertyValues("spring.datasource.generate-unique-name=true");

	@Test
	public void noDataSource() {
		this.contextRunner.run((context) -> assertThat(context).doesNotHaveBean(Flyway.class));
	}

	@Test
	public void createDataSourceWithUrl() {
		this.contextRunner.withUserConfiguration(EmbeddedDataSourceConfiguration.class)
				.withPropertyValues("spring.flyway.url:jdbc:hsqldb:mem:flywaytest").run((context) -> {
			assertThat(context).hasSingleBean(Flyway.class);
			assertThat(context.getBean(Flyway.class).getDataSource()).isNotNull();
		});
	}

	@Test
	public void createDataSourceWithUser() {
		this.contextRunner.withUserConfiguration(EmbeddedDataSourceConfiguration.class)
				.withPropertyValues("spring.datasource.url:jdbc:hsqldb:mem:normal", "spring.flyway.user:sa")
				.run((context) -> {
					assertThat(context).hasSingleBean(Flyway.class);
					assertThat(context.getBean(Flyway.class).getDataSource()).isNotNull();
				});
	}

	@Test
	public void createDataSourceFallbackToEmbeddedProperties() {
		this.contextRunner.withUserConfiguration(EmbeddedDataSourceConfiguration.class)
				.withPropertyValues("spring.flyway.url:jdbc:hsqldb:mem:flywaytest").run((context) -> {
			assertThat(context).hasSingleBean(Flyway.class);
			DataSource dataSource = context.getBean(Flyway.class).getDataSource();
			assertThat(dataSource).isNotNull();
			assertThat(dataSource).hasFieldOrPropertyWithValue("user", "sa");
			assertThat(dataSource).hasFieldOrPropertyWithValue("password", "");
		});
	}

	@Test
	public void createDataSourceWithUserAndFallbackToEmbeddedProperties() {
		this.contextRunner.withUserConfiguration(EmbeddedDataSourceConfiguration.class)
				.withPropertyValues("spring.flyway.user:sa").run((context) -> {
			assertThat(context).hasSingleBean(Flyway.class);
			DataSource dataSource = context.getBean(Flyway.class).getDataSource();
			assertThat(dataSource).isNotNull();
			assertThat(dataSource).extracting("url").hasSize(1).first().asString().startsWith("jdbc:h2:mem:");
		});
	}

	@Test
	public void flywayDataSource() {
		this.contextRunner
				.withUserConfiguration(FlywayDataSourceConfiguration.class, EmbeddedDataSourceConfiguration.class)
				.run((context) -> {
					assertThat(context).hasSingleBean(Flyway.class);
					assertThat(context.getBean(Flyway.class).getDataSource())
							.isEqualTo(context.getBean("flywayDataSource"));
				});
	}

	@Test
	public void flywayDataSourceWithoutDataSourceAutoConfiguration() {
		this.contextRunner.withUserConfiguration(FlywayDataSourceConfiguration.class).run((context) -> {
			assertThat(context).hasSingleBean(Flyway.class);
			assertThat(context.getBean(Flyway.class).getDataSource()).isEqualTo(context.getBean("flywayDataSource"));
		});
	}

	@Test
	public void schemaManagementProviderDetectsDataSource() {
		this.contextRunner
				.withUserConfiguration(FlywayDataSourceConfiguration.class, EmbeddedDataSourceConfiguration.class)
				.run((context) -> {
					FlywaySchemaManagementProvider schemaManagementProvider = context
							.getBean(FlywaySchemaManagementProvider.class);
					assertThat(schemaManagementProvider.getSchemaManagement(context.getBean(DataSource.class)))
							.isEqualTo(SchemaManagement.UNMANAGED);
					assertThat(schemaManagementProvider
							.getSchemaManagement(context.getBean("flywayDataSource", DataSource.class)))
							.isEqualTo(SchemaManagement.MANAGED);
				});
	}

	@Test
	public void defaultFlyway() {
		this.contextRunner.withUserConfiguration(EmbeddedDataSourceConfiguration.class).run((context) -> {
			assertThat(context).hasSingleBean(Flyway.class);
			Flyway flyway = context.getBean(Flyway.class);
			assertThat(flyway.getLocations()).containsExactly(new Location("classpath:db/migration"));
		});
	}

	@Test
	public void overrideLocations() {
		this.contextRunner.withUserConfiguration(EmbeddedDataSourceConfiguration.class)
				.withPropertyValues("spring.flyway.locations:classpath:db/changelog,classpath:db/migration")
				.run((context) -> {
					assertThat(context).hasSingleBean(Flyway.class);
					Flyway flyway = context.getBean(Flyway.class);
					assertThat(flyway.getLocations()).containsExactly(new Location("classpath:db/changelog"),
							new Location("classpath:db/migration"));
				});
	}

	@Test
	public void overrideLocationsList() {
		this.contextRunner.withUserConfiguration(EmbeddedDataSourceConfiguration.class)
				.withPropertyValues("spring.flyway.locations[0]:classpath:db/changelog",
						"spring.flyway.locations[1]:classpath:db/migration")
				.run((context) -> {
					assertThat(context).hasSingleBean(Flyway.class);
					Flyway flyway = context.getBean(Flyway.class);
					assertThat(flyway.getLocations()).containsExactly(new Location("classpath:db/changelog"),
							new Location("classpath:db/migration"));
				});
	}

	@Test
	public void overrideSchemas() {
		this.contextRunner.withUserConfiguration(EmbeddedDataSourceConfiguration.class)
				.withPropertyValues("spring.flyway.schemas:public").run((context) -> {
			assertThat(context).hasSingleBean(Flyway.class);
			Flyway flyway = context.getBean(Flyway.class);
			assertThat(Arrays.asList(flyway.getSchemas()).toString()).isEqualTo("[public]");
		});
	}

	@Test
	public void changeLogDoesNotExist() {
		this.contextRunner.withUserConfiguration(EmbeddedDataSourceConfiguration.class)
				.withPropertyValues("spring.flyway.locations:filesystem:no-such-dir").run((context) -> {
			assertThat(context).hasFailed();
			assertThat(context).getFailure().isInstanceOf(BeanCreationException.class);
		});
	}

	@Test
	public void checkLocationsAllMissing() {
		this.contextRunner.withUserConfiguration(EmbeddedDataSourceConfiguration.class)
				.withPropertyValues("spring.flyway.locations:classpath:db/missing1,classpath:db/migration2")
				.run((context) -> {
					assertThat(context).hasFailed();
					assertThat(context).getFailure().isInstanceOf(BeanCreationException.class);
					assertThat(context).getFailure().hasMessageContaining("Cannot find migrations location in");
				});
	}

	@Test
	public void checkLocationsAllExist() {
		this.contextRunner.withUserConfiguration(EmbeddedDataSourceConfiguration.class)
				.withPropertyValues("spring.flyway.locations:classpath:db/changelog,classpath:db/migration")
				.run((context) -> assertThat(context).hasNotFailed());
	}

	@Test
	public void checkLocationsAllExistWithImplicitClasspathPrefix() {
		this.contextRunner.withUserConfiguration(EmbeddedDataSourceConfiguration.class)
				.withPropertyValues("spring.flyway.locations:db/changelog,db/migration")
				.run((context) -> assertThat(context).hasNotFailed());
	}

	@Test
	public void checkLocationsAllExistWithFilesystemPrefix() {
		this.contextRunner.withUserConfiguration(EmbeddedDataSourceConfiguration.class)
				.withPropertyValues("spring.flyway.locations:filesystem:src/test/resources/db/migration")
				.run((context) -> assertThat(context).hasNotFailed());
	}

	@Test
	public void customFlywayMigrationStrategy() {
		this.contextRunner
				.withUserConfiguration(EmbeddedDataSourceConfiguration.class, MockFlywayMigrationStrategy.class)
				.run((context) -> {
					assertThat(context).hasSingleBean(Flyway.class);
					context.getBean(MockFlywayMigrationStrategy.class).assertCalled();
				});
	}

	@Test
	public void customFlywayMigrationInitializer() {
		this.contextRunner
				.withUserConfiguration(EmbeddedDataSourceConfiguration.class, CustomFlywayMigrationInitializer.class)
				.run((context) -> {
					assertThat(context).hasSingleBean(Flyway.class);
					FlywayMigrationInitializer initializer = context.getBean(FlywayMigrationInitializer.class);
					assertThat(initializer.getOrder()).isEqualTo(Ordered.HIGHEST_PRECEDENCE);
				});
	}

	@Test
	public void customFlywayMigrationInitializerWithJpa() {
		this.contextRunner
				.withUserConfiguration(EmbeddedDataSourceConfiguration.class,
						CustomFlywayMigrationInitializerWithJpaConfiguration.class)
				.run((context) -> assertThat(context).hasNotFailed());
	}

	@Test
	public void customFlywayMigrationInitializerWithJdbc() {
		this.contextRunner
				.withUserConfiguration(EmbeddedDataSourceConfiguration.class,
						CustomFlywayMigrationInitializerWithJdbcConfiguration.class)
				.run((context) -> assertThat(context).hasNotFailed());
	}

	@Test
	public void customFlywayWithJpa() {
		this.contextRunner
				.withUserConfiguration(EmbeddedDataSourceConfiguration.class, CustomFlywayWithJpaConfiguration.class)
				.run((context) -> assertThat(context).hasNotFailed());
	}

	@Test
	public void customFlywayWithJdbc() {
		this.contextRunner
				.withUserConfiguration(EmbeddedDataSourceConfiguration.class, CustomFlywayWithJdbcConfiguration.class)
				.run((context) -> assertThat(context).hasNotFailed());
	}

	@Test
	public void overrideBaselineVersionString() {
		this.contextRunner.withUserConfiguration(EmbeddedDataSourceConfiguration.class)
				.withPropertyValues("spring.flyway.baseline-version=0").run((context) -> {
			assertThat(context).hasSingleBean(Flyway.class);
			Flyway flyway = context.getBean(Flyway.class);
			assertThat(flyway.getBaselineVersion()).isEqualTo(MigrationVersion.fromVersion("0"));
		});
	}

	@Test
	public void overrideBaselineVersionNumber() {
		this.contextRunner.withUserConfiguration(EmbeddedDataSourceConfiguration.class)
				.withPropertyValues("spring.flyway.baseline-version=1").run((context) -> {
			assertThat(context).hasSingleBean(Flyway.class);
			Flyway flyway = context.getBean(Flyway.class);
			assertThat(flyway.getBaselineVersion()).isEqualTo(MigrationVersion.fromVersion("1"));
		});
	}

	@Test
	public void useVendorDirectory() {
		this.contextRunner.withUserConfiguration(EmbeddedDataSourceConfiguration.class)
				.withPropertyValues("spring.flyway.locations=classpath:db/vendors/{vendor},classpath:db/changelog")
				.run((context) -> {
					assertThat(context).hasSingleBean(Flyway.class);
					Flyway flyway = context.getBean(Flyway.class);
					assertThat(flyway.getLocations()).containsExactlyInAnyOrder(new Location("classpath:db/vendors/h2"),
							new Location("classpath:db/changelog"));
				});
	}

	@Test
	public void useOneLocationWithVendorDirectory() {
		this.contextRunner.withUserConfiguration(EmbeddedDataSourceConfiguration.class)
				.withPropertyValues("spring.flyway.locations=classpath:db/vendors/{vendor}").run((context) -> {
			assertThat(context).hasSingleBean(Flyway.class);
			Flyway flyway = context.getBean(Flyway.class);
			assertThat(flyway.getLocations()).containsExactly(new Location("classpath:db/vendors/h2"));
		});
	}

	@Test
	public void callbacksAreConfiguredAndOrdered() {
		this.contextRunner.withUserConfiguration(EmbeddedDataSourceConfiguration.class, CallbackConfiguration.class)
				.run((context) -> {
					assertThat(context).hasSingleBean(Flyway.class);
					Flyway flyway = context.getBean(Flyway.class);
					Callback callbackOne = context.getBean("callbackOne", Callback.class);
					Callback callbackTwo = context.getBean("callbackTwo", Callback.class);
					assertThat(flyway.getCallbacks()).hasSize(2);
					assertThat(flyway.getCallbacks()).containsExactly(callbackTwo, callbackOne);
					InOrder orderedCallbacks = inOrder(callbackOne, callbackTwo);
					orderedCallbacks.verify(callbackTwo).handle(any(Event.class), any(Context.class));
					orderedCallbacks.verify(callbackOne).handle(any(Event.class), any(Context.class));
				});
	}

	@Test
	public void legacyCallbacksAreConfiguredAndOrdered() {
		this.contextRunner
				.withUserConfiguration(EmbeddedDataSourceConfiguration.class, LegacyCallbackConfiguration.class)
				.run((context) -> {
					assertThat(context).hasSingleBean(Flyway.class);
					Flyway flyway = context.getBean(Flyway.class);
					FlywayCallback callbackOne = context.getBean("legacyCallbackOne", FlywayCallback.class);
					FlywayCallback callbackTwo = context.getBean("legacyCallbackTwo", FlywayCallback.class);
					assertThat(flyway.getCallbacks()).hasSize(2);
					InOrder orderedCallbacks = inOrder(callbackOne, callbackTwo);
					orderedCallbacks.verify(callbackTwo).beforeMigrate(any(Connection.class));
					orderedCallbacks.verify(callbackOne).beforeMigrate(any(Connection.class));
				});
	}

	@Test
	public void callbacksAndLegacyCallbacksCannotBeMixed() {
		this.contextRunner.withUserConfiguration(EmbeddedDataSourceConfiguration.class,
				LegacyCallbackConfiguration.class, CallbackConfiguration.class).run((context) -> {
			assertThat(context).hasFailed();
			assertThat(context.getStartupFailure())
					.hasMessageContaining("Found a mixture of Callback and FlywayCallback beans."
							+ " One type must be used exclusively.");
		});
	}

	@Test
	public void configurationCustomizersAreConfiguredAndOrdered() {
		this.contextRunner.withUserConfiguration(EmbeddedDataSourceConfiguration.class,
				ConfigurationCustomizerConfiguration.class).run((context) -> {
			assertThat(context).hasSingleBean(Flyway.class);
			Flyway flyway = context.getBean(Flyway.class);
			assertThat(flyway.getConfiguration().getConnectRetries()).isEqualTo(5);
			assertThat(flyway.getConfiguration().isIgnoreMissingMigrations()).isTrue();
			assertThat(flyway.getConfiguration().isIgnorePendingMigrations()).isTrue();
		});
	}

	@Configuration
	protected static class FlywayDataSourceConfiguration {

		@Bean
		@Primary
		public DataSource normalDataSource() {
			return DataSourceBuilder.create().url("jdbc:hsqldb:mem:normal").username("sa").build();
		}

		@FlywayDataSource
		@Bean
		public DataSource flywayDataSource() {
			return DataSourceBuilder.create().url("jdbc:hsqldb:mem:flywaytest").username("sa").build();
		}

	}

	@Configuration
	protected static class CustomFlywayMigrationInitializer {

		@Bean
		public FlywayMigrationInitializer flywayMigrationInitializer(Flyway flyway) {
			FlywayMigrationInitializer initializer = new FlywayMigrationInitializer(flyway);
			initializer.setOrder(Ordered.HIGHEST_PRECEDENCE);
			return initializer;
		}

	}

	@Configuration
	protected static class CustomFlywayMigrationInitializerWithJpaConfiguration {

		@Bean
		public FlywayMigrationInitializer customFlywayMigrationInitializer(Flyway flyway) {
			return new FlywayMigrationInitializer(flyway);
		}

		@Bean
		public LocalContainerEntityManagerFactoryBean entityManagerFactoryBean(DataSource dataSource) {
			Map<String, Object> properties = new HashMap<>();
			properties.put("configured", "manually");
			properties.put("hibernate.transaction.jta.platform", NoJtaPlatform.INSTANCE);
			return new EntityManagerFactoryBuilder(new HibernateJpaVendorAdapter(), properties, null)
					.dataSource(dataSource).build();
		}

	}

	@Configuration
	protected static class CustomFlywayWithJpaConfiguration {

		private final DataSource dataSource;

		protected CustomFlywayWithJpaConfiguration(DataSource dataSource) {
			this.dataSource = dataSource;
		}

		@Bean
		public Flyway customFlyway() {
			return new Flyway();
		}

		@Bean
		public LocalContainerEntityManagerFactoryBean entityManagerFactoryBean() {
			Map<String, Object> properties = new HashMap<>();
			properties.put("configured", "manually");
			properties.put("hibernate.transaction.jta.platform", NoJtaPlatform.INSTANCE);
			return new EntityManagerFactoryBuilder(new HibernateJpaVendorAdapter(), properties, null)
					.dataSource(this.dataSource).build();
		}

	}

	@Configuration
	protected static class CustomFlywayWithJdbcConfiguration {

		private final DataSource dataSource;

		protected CustomFlywayWithJdbcConfiguration(DataSource dataSource) {
			this.dataSource = dataSource;
		}

		@Bean
		public Flyway customFlyway() {
			return new Flyway();
		}

		@Bean
		public JdbcOperations jdbcOperations() {
			return new JdbcTemplate(this.dataSource);
		}

		@Bean
		public NamedParameterJdbcOperations namedParameterJdbcOperations() {
			return new NamedParameterJdbcTemplate(this.dataSource);
		}

	}

	@Configuration
	protected static class CustomFlywayMigrationInitializerWithJdbcConfiguration {

		private final DataSource dataSource;

		protected CustomFlywayMigrationInitializerWithJdbcConfiguration(DataSource dataSource) {
			this.dataSource = dataSource;
		}

		@Bean
		public FlywayMigrationInitializer customFlywayMigrationInitializer(Flyway flyway) {
			return new FlywayMigrationInitializer(flyway);
		}

		@Bean
		public JdbcOperations jdbcOperations() {
			return new JdbcTemplate(this.dataSource);
		}

		@Bean
		public NamedParameterJdbcOperations namedParameterJdbcOperations() {
			return new NamedParameterJdbcTemplate(this.dataSource);
		}

	}

	@Component
	protected static class MockFlywayMigrationStrategy implements FlywayMigrationStrategy {

		private boolean called = false;

		@Override
		public void migrate(Flyway flyway) {
			this.called = true;
		}

		public void assertCalled() {
			assertThat(this.called).isTrue();
		}

	}

	@Configuration
	static class CallbackConfiguration {

		@Bean
		@Order(1)
		public Callback callbackOne() {
			return mockCallback();
		}

		@Bean
		@Order(0)
		public Callback callbackTwo() {
			return mockCallback();
		}

		private Callback mockCallback() {
			Callback callback = mock(Callback.class);
			given(callback.supports(any(Event.class), any(Context.class))).willReturn(true);
			return callback;
		}

	}

	@Configuration
	static class LegacyCallbackConfiguration {

		@Bean
		@Order(1)
		public FlywayCallback legacyCallbackOne() {
			return mock(FlywayCallback.class);
		}

		@Bean
		@Order(0)
		public FlywayCallback legacyCallbackTwo() {
			return mock(FlywayCallback.class);
		}

	}

	@Configuration
	static class ConfigurationCustomizerConfiguration {

		@Bean
		@Order(1)
		public FlywayConfigurationCustomizer customizerOne() {
			return (configuration) -> configuration.connectRetries(5).ignorePendingMigrations(true);
		}

		@Bean
		@Order(0)
		public FlywayConfigurationCustomizer customizerTwo() {
			return (configuration) -> configuration.connectRetries(10).ignoreMissingMigrations(true);
		}

	}

}
