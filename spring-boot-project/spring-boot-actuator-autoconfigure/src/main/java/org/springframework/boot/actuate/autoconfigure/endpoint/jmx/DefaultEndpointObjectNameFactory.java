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

package org.springframework.boot.actuate.autoconfigure.endpoint.jmx;

import org.springframework.boot.actuate.endpoint.jmx.EndpointObjectNameFactory;
import org.springframework.boot.actuate.endpoint.jmx.ExposableJmxEndpoint;
import org.springframework.core.env.Environment;
import org.springframework.jmx.support.ObjectNameManager;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

/**
 * A {@link EndpointObjectNameFactory} that generates standard {@link ObjectName} for
 * Actuator's endpoints.
 *
 * @author Stephane Nicoll
 */
class DefaultEndpointObjectNameFactory implements EndpointObjectNameFactory {

	private final JmxEndpointProperties properties;

	private final MBeanServer mBeanServer;

	private final String contextId;

	private final boolean uniqueNames;

	DefaultEndpointObjectNameFactory(JmxEndpointProperties properties, Environment environment, MBeanServer mBeanServer,
									 String contextId) {
		this.properties = properties;
		this.mBeanServer = mBeanServer;
		this.contextId = contextId;
		this.uniqueNames = determineUniqueNames(environment, properties);
	}

	@SuppressWarnings("deprecation")
	private static boolean determineUniqueNames(Environment environment, JmxEndpointProperties properties) {
		Boolean uniqueNames = environment.getProperty("spring.jmx.unique-names", Boolean.class);
		Boolean endpointUniqueNames = properties.getUniqueNames();
		if (uniqueNames == null) {
			return (endpointUniqueNames != null) ? endpointUniqueNames : false;
		}
		if (endpointUniqueNames != null & !uniqueNames.equals(endpointUniqueNames)) {
			throw new IllegalArgumentException(
					"Configuration mismatch, 'management.endpoints.jmx.unique-names' is deprecated, use only 'spring.jmx.unique-names'");
		}
		return uniqueNames;
	}

	@Override
	public ObjectName getObjectName(ExposableJmxEndpoint endpoint) throws MalformedObjectNameException {
		StringBuilder builder = new StringBuilder(this.properties.getDomain());
		builder.append(":type=Endpoint");
		builder.append(",name=").append(StringUtils.capitalize(endpoint.getEndpointId().toString()));
		String baseName = builder.toString();
		if (this.mBeanServer != null && hasMBean(baseName)) {
			builder.append(",context=").append(this.contextId);
		}
		if (this.uniqueNames) {
			String identity = ObjectUtils.getIdentityHexString(endpoint);
			builder.append(",identity=").append(identity);
		}
		builder.append(getStaticNames());
		return ObjectNameManager.getInstance(builder.toString());
	}

	private boolean hasMBean(String baseObjectName) throws MalformedObjectNameException {
		ObjectName query = new ObjectName(baseObjectName + ",*");
		return !this.mBeanServer.queryNames(query, null).isEmpty();
	}

	private String getStaticNames() {
		if (this.properties.getStaticNames().isEmpty()) {
			return "";
		}
		StringBuilder builder = new StringBuilder();
		this.properties.getStaticNames()
				.forEach((name, value) -> builder.append(",").append(name).append("=").append(value));
		return builder.toString();
	}

}
