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

package org.springframework.boot.test.context;

import org.springframework.boot.SpringBootConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.AliasFor;

import java.lang.annotation.*;

/**
 * {@link Configuration @Configuration} that can be used to define additional beans or
 * customizations for a test. Unlike regular {@code @Configuration} classes the use of
 * {@code @TestConfiguration} does not prevent auto-detection of
 * {@link SpringBootConfiguration @SpringBootConfiguration}.
 *
 * @author Phillip Webb
 * @see SpringBootTestContextBootstrapper
 * @since 1.4.0
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Configuration
@TestComponent
public @interface TestConfiguration {

	/**
	 * Explicitly specify the name of the Spring bean definition associated with this
	 * Configuration class. See {@link Configuration#value()} for details.
	 *
	 * @return the specified bean name, if any
	 */
	@AliasFor(annotation = Configuration.class)
	String value() default "";

}
