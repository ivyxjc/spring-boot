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

package org.springframework.boot.test.autoconfigure;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;

import java.lang.annotation.*;

/**
 * Annotation that can be used to override
 * {@link EnableAutoConfiguration @EnableAutoConfiguration}. Often used in combination
 * with {@link ImportAutoConfiguration} to limit the auto-configuration classes that are
 * loaded.
 *
 * @author Phillip Webb
 * @see EnableAutoConfiguration#ENABLED_OVERRIDE_PROPERTY
 * @since 1.4.0
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface OverrideAutoConfiguration {

	/**
	 * The value of the {@link EnableAutoConfiguration#ENABLED_OVERRIDE_PROPERTY enabled
	 * override property}.
	 *
	 * @return the override value
	 */
	boolean enabled();

}
