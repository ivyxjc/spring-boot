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

package org.springframework.boot.testsupport.testcontainers;

import org.testcontainers.containers.GenericContainer;

import java.time.Duration;

/**
 * A {@link GenericContainer} for Redis.
 *
 * @author Andy Wilkinson
 * @author Madhura Bhave
 * @since 2.0.0
 */
public class RedisContainer extends Container {

	public RedisContainer() {
		super("redis:4.0.6", 6379,
				(container) -> container.withStartupAttempts(5).withStartupTimeout(Duration.ofMinutes(2)));
	}

}
