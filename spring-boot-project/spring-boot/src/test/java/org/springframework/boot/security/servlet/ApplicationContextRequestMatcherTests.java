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

package org.springframework.boot.security.servlet;

import org.junit.Test;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.context.ApplicationContext;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockServletContext;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.StaticWebApplicationContext;

import javax.servlet.http.HttpServletRequest;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests for {@link ApplicationContextRequestMatcher}.
 *
 * @author Phillip Webb
 */
public class ApplicationContextRequestMatcherTests {

	@Test
	public void createWhenContextClassIsNullShouldThrowException() {
		assertThatIllegalArgumentException().isThrownBy(() -> new TestApplicationContextRequestMatcher<>(null))
				.withMessageContaining("Context class must not be null");
	}

	@Test
	public void matchesWhenContextClassIsApplicationContextShouldProvideContext() {
		StaticWebApplicationContext context = createWebApplicationContext();
		assertThat(new TestApplicationContextRequestMatcher<>(ApplicationContext.class)
				.callMatchesAndReturnProvidedContext(context).get()).isEqualTo(context);
	}

	@Test
	public void matchesWhenContextClassIsExistingBeanShouldProvideBean() {
		StaticWebApplicationContext context = createWebApplicationContext();
		context.registerSingleton("existingBean", ExistingBean.class);
		assertThat(new TestApplicationContextRequestMatcher<>(ExistingBean.class)
				.callMatchesAndReturnProvidedContext(context).get()).isEqualTo(context.getBean(ExistingBean.class));
	}

	@Test
	public void matchesWhenContextClassIsBeanThatDoesNotExistShouldSupplyException() {
		StaticWebApplicationContext context = createWebApplicationContext();
		Supplier<ExistingBean> supplier = new TestApplicationContextRequestMatcher<>(ExistingBean.class)
				.callMatchesAndReturnProvidedContext(context);
		assertThatExceptionOfType(NoSuchBeanDefinitionException.class).isThrownBy(supplier::get);
	}

	@Test // gh-18012
	public void machesWhenCalledWithDifferentApplicationContextDoesNotCache() {
		StaticWebApplicationContext context1 = createWebApplicationContext();
		StaticWebApplicationContext context2 = createWebApplicationContext();
		TestApplicationContextRequestMatcher<ApplicationContext> matcher = new TestApplicationContextRequestMatcher<>(
				ApplicationContext.class);
		assertThat(matcher.callMatchesAndReturnProvidedContext(context1).get()).isEqualTo(context1);
		assertThat(matcher.callMatchesAndReturnProvidedContext(context2).get()).isEqualTo(context2);
	}

	@Test
	public void initializeAndMatchesAreNotCalledIfContextIsIgnored() {
		StaticWebApplicationContext context = createWebApplicationContext();
		TestApplicationContextRequestMatcher<ApplicationContext> matcher = new TestApplicationContextRequestMatcher<ApplicationContext>(
				ApplicationContext.class) {

			@Override
			protected boolean ignoreApplicationContext(WebApplicationContext webApplicationContext) {
				return true;
			}

			@Override
			protected void initialized(Supplier<ApplicationContext> context) {
				throw new IllegalStateException();
			}

			@Override
			protected boolean matches(HttpServletRequest request, Supplier<ApplicationContext> context) {
				throw new IllegalStateException();
			}

		};
		MockHttpServletRequest request = new MockHttpServletRequest(context.getServletContext());
		assertThat(matcher.matches(request)).isFalse();
	}

	private StaticWebApplicationContext createWebApplicationContext() {
		StaticWebApplicationContext context = new StaticWebApplicationContext();
		MockServletContext servletContext = new MockServletContext();
		context.setServletContext(servletContext);
		servletContext.setAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE, context);
		return context;
	}

	static class ExistingBean {

	}

	static class NewBean {

		private final ExistingBean bean;

		NewBean(ExistingBean bean) {
			this.bean = bean;
		}

		public ExistingBean getBean() {
			return this.bean;
		}

	}

	static class TestApplicationContextRequestMatcher<C> extends ApplicationContextRequestMatcher<C> {

		private Supplier<C> providedContext;

		TestApplicationContextRequestMatcher(Class<? extends C> context) {
			super(context);
		}

		public Supplier<C> callMatchesAndReturnProvidedContext(WebApplicationContext context) {
			return callMatchesAndReturnProvidedContext(new MockHttpServletRequest(context.getServletContext()));
		}

		public Supplier<C> callMatchesAndReturnProvidedContext(HttpServletRequest request) {
			matches(request);
			return getProvidedContext();
		}

		@Override
		protected boolean matches(HttpServletRequest request, Supplier<C> context) {
			this.providedContext = context;
			return false;
		}

		public Supplier<C> getProvidedContext() {
			return this.providedContext;
		}

	}

}
