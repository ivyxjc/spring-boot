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

package org.springframework.boot.load.it.props;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.util.Properties;

/**
 * Spring configuration.
 *
 * @author Dave Syer
 */
@Configuration
@ComponentScan
public class SpringConfiguration implements InitializingBean {

	private String message = "Jar";

	@Override
	public void afterPropertiesSet() throws IOException {
		Properties props = new Properties();
		props.load(new ClassPathResource("application.properties").getInputStream());
		String value = props.getProperty("message");
		if (value != null) {
			this.message = value;
		}

	}

	public void run(String... args) {
		System.err.println("Hello Embedded " + this.message + "!");
	}

}
