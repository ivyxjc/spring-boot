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

package org.springframework.boot.actuate.autoconfigure.elasticsearch;

import org.springframework.boot.actuate.elasticsearch.ElasticsearchHealthIndicator;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * External configuration properties for {@link ElasticsearchHealthIndicator}.
 *
 * @author Binwei Yang
 * @author Andy Wilkinson
 * @since 2.0.0
 */
@ConfigurationProperties(prefix = "management.health.elasticsearch", ignoreUnknownFields = false)
public class ElasticsearchHealthIndicatorProperties {

	/**
	 * Comma-separated index names.
	 */
	private List<String> indices = new ArrayList<>();

	/**
	 * Time to wait for a response from the cluster.
	 */
	private Duration responseTimeout = Duration.ofMillis(100);

	public List<String> getIndices() {
		return this.indices;
	}

	public void setIndices(List<String> indices) {
		this.indices = indices;
	}

	public Duration getResponseTimeout() {
		return this.responseTimeout;
	}

	public void setResponseTimeout(Duration responseTimeout) {
		this.responseTimeout = responseTimeout;
	}

}
