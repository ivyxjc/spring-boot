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

package org.springframework.boot.autoconfigure.admin;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.admin.SpringApplicationAdminMXBean;
import org.springframework.boot.admin.SpringApplicationAdminMXBeanRegistrar;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.jmx.JmxAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.jmx.export.MBeanExporter;

import javax.management.MalformedObjectNameException;

/**
 * Register a JMX component that allows to administer the current application. Intended
 * for internal use only.
 *
 * @author Stephane Nicoll
 * @author Andy Wilkinson
 * @see SpringApplicationAdminMXBean
 * @since 1.3.0
 */
@Configuration
@AutoConfigureAfter(JmxAutoConfiguration.class)
@ConditionalOnProperty(prefix = "spring.application.admin", value = "enabled", havingValue = "true",
					   matchIfMissing = false)
public class SpringApplicationAdminJmxAutoConfiguration {

	/**
	 * The property to use to customize the {@code ObjectName} of the application admin
	 * mbean.
	 */
	private static final String JMX_NAME_PROPERTY = "spring.application.admin.jmx-name";

	/**
	 * The default {@code ObjectName} of the application admin mbean.
	 */
	private static final String DEFAULT_JMX_NAME = "org.springframework.boot:type=Admin,name=SpringApplication";

	private final Iterable<MBeanExporter> mbeanExporters;

	private final Environment environment;

	public SpringApplicationAdminJmxAutoConfiguration(ObjectProvider<MBeanExporter> mbeanExporters,
													  Environment environment) {
		this.mbeanExporters = mbeanExporters;
		this.environment = environment;
	}

	@Bean
	@ConditionalOnMissingBean
	public SpringApplicationAdminMXBeanRegistrar springApplicationAdminRegistrar() throws MalformedObjectNameException {
		String jmxName = this.environment.getProperty(JMX_NAME_PROPERTY, DEFAULT_JMX_NAME);
		if (this.mbeanExporters != null) { // Make sure to not register that MBean twice
			for (MBeanExporter mbeanExporter : this.mbeanExporters) {
				mbeanExporter.addExcludedBean(jmxName);
			}
		}
		return new SpringApplicationAdminMXBeanRegistrar(jmxName);
	}

}
