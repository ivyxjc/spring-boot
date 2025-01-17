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
package org.springframework.boot.actuate.autoconfigure.couchbase;

import com.couchbase.client.java.Cluster;
import org.springframework.boot.actuate.autoconfigure.health.CompositeHealthIndicatorConfiguration;
import org.springframework.boot.actuate.autoconfigure.health.ConditionalOnEnabledHealthIndicator;
import org.springframework.boot.actuate.autoconfigure.health.HealthIndicatorAutoConfiguration;
import org.springframework.boot.actuate.couchbase.CouchbaseHealthIndicator;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.couchbase.CouchbaseAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for
 * {@link CouchbaseHealthIndicator}.
 *
 * @author Eddú Meléndez
 * @author Stephane Nicoll
 * @author Andy Wilkinson Nicoll
 * @since 2.0.0
 */
@Configuration
@ConditionalOnClass(Cluster.class)
@ConditionalOnBean(Cluster.class)
@ConditionalOnEnabledHealthIndicator("couchbase")
@AutoConfigureBefore(HealthIndicatorAutoConfiguration.class)
@AutoConfigureAfter(CouchbaseAutoConfiguration.class)
public class CouchbaseHealthIndicatorAutoConfiguration
		extends CompositeHealthIndicatorConfiguration<CouchbaseHealthIndicator, Cluster> {

	private final Map<String, Cluster> clusters;

	public CouchbaseHealthIndicatorAutoConfiguration(Map<String, Cluster> clusters) {
		this.clusters = clusters;
	}

	@Bean
	@ConditionalOnMissingBean(name = "couchbaseHealthIndicator")
	public HealthIndicator couchbaseHealthIndicator() {
		return createHealthIndicator(this.clusters);
	}

	@Override
	protected CouchbaseHealthIndicator createHealthIndicator(Cluster cluster) {
		return new CouchbaseHealthIndicator(cluster);
	}

}
