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

package org.springframework.boot.test.autoconfigure.filter;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.TypeExcludeFilter;

import java.lang.annotation.*;

/**
 * Annotation that can be on tests to define a set of {@link TypeExcludeFilter} classes
 * that should be applied to {@link SpringBootApplication @SpringBootApplication}
 * component scanning.
 *
 * @author Phillip Webb
 * @see TypeExcludeFilter
 * @since 1.4.0
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface TypeExcludeFilters {

	/**
	 * Specifies {@link TypeExcludeFilter} classes that should be applied to
	 * {@link SpringBootApplication @SpringBootApplication} component scanning. Classes
	 * specified here can either have a no-arg constructor or accept a single
	 * {@code Class<?>} argument if they need access to the {@code testClass}.
	 *
	 * @return the type exclude filters to apply
	 * @see TypeExcludeFilter
	 */
	Class<? extends TypeExcludeFilter>[] value();

}
