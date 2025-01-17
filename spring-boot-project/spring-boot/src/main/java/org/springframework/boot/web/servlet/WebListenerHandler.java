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

package org.springframework.boot.web.servlet;

import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.annotation.ScannedGenericBeanDefinition;

import javax.servlet.annotation.WebListener;
import java.util.Map;

/**
 * Handler for {@link WebListener}-annotated classes.
 *
 * @author Andy Wilkinson
 */
class WebListenerHandler extends ServletComponentHandler {

	WebListenerHandler() {
		super(WebListener.class);
	}

	@Override
	protected void doHandle(Map<String, Object> attributes, ScannedGenericBeanDefinition beanDefinition,
							BeanDefinitionRegistry registry) {
		BeanDefinitionBuilder builder = BeanDefinitionBuilder.rootBeanDefinition(ServletListenerRegistrationBean.class);
		builder.addPropertyValue("listener", beanDefinition);
		registry.registerBeanDefinition(beanDefinition.getBeanClassName(), builder.getBeanDefinition());
	}

}
