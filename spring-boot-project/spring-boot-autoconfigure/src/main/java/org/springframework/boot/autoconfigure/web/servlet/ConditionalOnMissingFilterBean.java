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

package org.springframework.boot.autoconfigure.web.servlet;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Conditional;
import org.springframework.core.annotation.AliasFor;

import javax.servlet.Filter;
import java.lang.annotation.*;

/**
 * {@link Conditional} that only matches when no {@link Filter} beans of the specified
 * type are contained in the {@link BeanFactory}. This condition will detect both directly
 * register {@link Filter} beans as well as those registered via a
 * {@link FilterRegistrationBean}.
 * <p>
 * When placed on a {@code @Bean} method, the bean class defaults to the return type of
 * the factory method or the type of the {@link Filter} if the bean is a
 * {@link FilterRegistrationBean}:
 *
 * <pre class="code">
 * &#064;Configuration
 * public class MyAutoConfiguration {
 *
 *     &#064;ConditionalOnMissingFilterBean
 *     &#064;Bean
 *     public MyFilter myFilter() {
 *         ...
 *     }
 *
 * }</pre>
 * <p>
 * In the sample above the condition will match if no bean of type {@code MyFilter} or
 * {@code FilterRegistrationBean<MyFilter>} is already contained in the
 * {@link BeanFactory}.
 *
 * @author Phillip Webb
 * @since 2.1.0
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@ConditionalOnMissingBean(parameterizedContainer = FilterRegistrationBean.class)
public @interface ConditionalOnMissingFilterBean {

	/**
	 * The filter bean type that must not be present.
	 *
	 * @return the bean type
	 */
	@AliasFor(annotation = ConditionalOnMissingBean.class)
	Class<? extends Filter>[] value() default {};

}
