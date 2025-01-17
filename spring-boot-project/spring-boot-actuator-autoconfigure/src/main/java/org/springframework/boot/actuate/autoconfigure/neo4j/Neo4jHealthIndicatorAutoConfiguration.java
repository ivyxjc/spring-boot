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

package org.springframework.boot.actuate.autoconfigure.neo4j;

import org.neo4j.ogm.session.SessionFactory;
import org.springframework.boot.actuate.autoconfigure.health.CompositeHealthIndicatorConfiguration;
import org.springframework.boot.actuate.autoconfigure.health.ConditionalOnEnabledHealthIndicator;
import org.springframework.boot.actuate.autoconfigure.health.HealthIndicatorAutoConfiguration;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.actuate.neo4j.Neo4jHealthIndicator;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.data.neo4j.Neo4jDataAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for {@link Neo4jHealthIndicator}.
 *
 * @author Eric Spiegelberg
 * @author Stephane Nicoll
 * @since 2.0.0
 */
@Configuration
@ConditionalOnClass(SessionFactory.class)
@ConditionalOnBean(SessionFactory.class)
@ConditionalOnEnabledHealthIndicator("neo4j")
@AutoConfigureBefore(HealthIndicatorAutoConfiguration.class)
@AutoConfigureAfter(Neo4jDataAutoConfiguration.class)
public class Neo4jHealthIndicatorAutoConfiguration
		extends CompositeHealthIndicatorConfiguration<Neo4jHealthIndicator, SessionFactory> {

	private final Map<String, SessionFactory> sessionFactories;

	public Neo4jHealthIndicatorAutoConfiguration(Map<String, SessionFactory> sessionFactories) {
		this.sessionFactories = sessionFactories;
	}

	@Bean
	@ConditionalOnMissingBean(name = "neo4jHealthIndicator")
	public HealthIndicator neo4jHealthIndicator() {
		return createHealthIndicator(this.sessionFactories);
	}

}
