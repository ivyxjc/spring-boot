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

package org.springframework.boot.test.autoconfigure.data.jdbc;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseAutoConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import javax.sql.DataSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.springframework.boot.test.autoconfigure.AutoConfigurationImportedCondition.importedAutoConfiguration;

/**
 * Integration tests for {@link DataJdbcTest}.
 *
 * @author Andy Wilkinson
 */
@RunWith(SpringRunner.class)
@DataJdbcTest
@TestPropertySource(
		properties = "spring.datasource.schema=classpath:org/springframework/boot/test/autoconfigure/data/jdbc/schema.sql")
public class DataJdbcTestIntegrationTests {

	@Autowired
	private ExampleRepository repository;

	@Autowired
	private DataSource dataSource;

	@Autowired
	private ApplicationContext applicationContext;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Test
	public void testRepository() {
		this.jdbcTemplate.update("INSERT INTO EXAMPLE_ENTITY (id, name, reference) VALUES (1, 'a', 'alpha')");
		this.jdbcTemplate.update("INSERT INTO EXAMPLE_ENTITY (id, name, reference) VALUES (2, 'b', 'bravo')");
		assertThat(this.repository.findAll()).hasSize(2);
	}

	@Test
	public void replacesDefinedDataSourceWithEmbeddedDefault() throws Exception {
		String product = this.dataSource.getConnection().getMetaData().getDatabaseProductName();
		assertThat(product).isEqualTo("H2");
	}

	@Test
	public void didNotInjectExampleComponent() {
		assertThatExceptionOfType(NoSuchBeanDefinitionException.class)
				.isThrownBy(() -> this.applicationContext.getBean(ExampleComponent.class));
	}

	@Test
	public void flywayAutoConfigurationWasImported() {
		assertThat(this.applicationContext).has(importedAutoConfiguration(FlywayAutoConfiguration.class));
	}

	@Test
	public void liquibaseAutoConfigurationWasImported() {
		assertThat(this.applicationContext).has(importedAutoConfiguration(LiquibaseAutoConfiguration.class));
	}

}
