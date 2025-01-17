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

package org.springframework.boot.autoconfigure.jdbc;

import org.apache.commons.dbcp2.BasicDataSource;

import java.util.UUID;

/**
 * {@link BasicDataSource} used for testing.
 *
 * @author Phillip Webb
 * @author Kazuki Shimizu
 * @author Stephane Nicoll
 */
public class TestDataSource extends BasicDataSource {

	/**
	 * Create an in-memory database with the specified name.
	 *
	 * @param name the name of the database
	 */
	public TestDataSource(String name) {
		setDriverClassName("org.hsqldb.jdbcDriver");
		setUrl("jdbc:hsqldb:mem:" + name);
		setUsername("sa");
	}

	/**
	 * Create an in-memory database with a random name.
	 */
	public TestDataSource() {
		this(UUID.randomUUID().toString());
	}

}
