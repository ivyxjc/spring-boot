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

package org.springframework.boot.actuate.endpoint.web.jersey;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.model.Resource;
import org.glassfish.jersey.servlet.ServletContainer;
import org.springframework.boot.actuate.endpoint.web.EndpointLinksResolver;
import org.springframework.boot.actuate.endpoint.web.EndpointMapping;
import org.springframework.boot.actuate.endpoint.web.EndpointMediaTypes;
import org.springframework.boot.actuate.endpoint.web.annotation.AbstractWebEndpointIntegrationTests;
import org.springframework.boot.actuate.endpoint.web.annotation.WebEndpointDiscoverer;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.boot.web.servlet.context.AnnotationConfigServletWebServerApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.servletapi.SecurityContextHolderAwareRequestWrapper;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.ext.ContextResolver;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;

/**
 * Integration tests for web endpoints exposed using Jersey.
 *
 * @author Andy Wilkinson
 * @see JerseyEndpointResourceFactory
 */
public class JerseyWebEndpointIntegrationTests
		extends AbstractWebEndpointIntegrationTests<AnnotationConfigServletWebServerApplicationContext> {

	public JerseyWebEndpointIntegrationTests() {
		super(JerseyWebEndpointIntegrationTests::createApplicationContext,
				JerseyWebEndpointIntegrationTests::applyAuthenticatedConfiguration);
	}

	private static AnnotationConfigServletWebServerApplicationContext createApplicationContext() {
		AnnotationConfigServletWebServerApplicationContext context = new AnnotationConfigServletWebServerApplicationContext();
		context.register(JerseyConfiguration.class);
		return context;
	}

	private static void applyAuthenticatedConfiguration(AnnotationConfigServletWebServerApplicationContext context) {
		context.register(AuthenticatedConfiguration.class);
	}

	@Override
	protected int getPort(AnnotationConfigServletWebServerApplicationContext context) {
		return context.getWebServer().getPort();
	}

	@Override
	protected void validateErrorBody(WebTestClient.BodyContentSpec body, HttpStatus status, String path,
									 String message) {
		// Jersey doesn't support the general error page handling
	}

	@Configuration
	static class JerseyConfiguration {

		@Bean
		public TomcatServletWebServerFactory tomcat() {
			return new TomcatServletWebServerFactory(0);
		}

		@Bean
		public ServletRegistrationBean<ServletContainer> servletContainer(ResourceConfig resourceConfig) {
			return new ServletRegistrationBean<>(new ServletContainer(resourceConfig), "/*");
		}

		@Bean
		public ResourceConfig resourceConfig(Environment environment, WebEndpointDiscoverer endpointDiscoverer,
											 EndpointMediaTypes endpointMediaTypes) {
			ResourceConfig resourceConfig = new ResourceConfig();
			Collection<Resource> resources = new JerseyEndpointResourceFactory().createEndpointResources(
					new EndpointMapping(environment.getProperty("endpointPath")), endpointDiscoverer.getEndpoints(),
					endpointMediaTypes, new EndpointLinksResolver(endpointDiscoverer.getEndpoints()));
			resourceConfig.registerResources(new HashSet<>(resources));
			resourceConfig.register(JacksonFeature.class);
			resourceConfig.register(new ObjectMapperContextResolver(new ObjectMapper()), ContextResolver.class);
			return resourceConfig;
		}

	}

	@Configuration
	static class AuthenticatedConfiguration {

		@Bean
		public Filter securityFilter() {
			return new OncePerRequestFilter() {

				@Override
				protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
												FilterChain filterChain) throws ServletException, IOException {
					SecurityContext context = SecurityContextHolder.createEmptyContext();
					context.setAuthentication(new UsernamePasswordAuthenticationToken("Alice", "secret",
							Arrays.asList(new SimpleGrantedAuthority("ROLE_ACTUATOR"))));
					SecurityContextHolder.setContext(context);
					try {
						filterChain.doFilter(new SecurityContextHolderAwareRequestWrapper(request, "ROLE_"), response);
					} finally {
						SecurityContextHolder.clearContext();
					}
				}

			};
		}

	}

	private static final class ObjectMapperContextResolver implements ContextResolver<ObjectMapper> {

		private final ObjectMapper objectMapper;

		private ObjectMapperContextResolver(ObjectMapper objectMapper) {
			this.objectMapper = objectMapper;
		}

		@Override
		public ObjectMapper getContext(Class<?> type) {
			return this.objectMapper;
		}

	}

}
