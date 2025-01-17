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

package org.springframework.boot.actuate.autoconfigure.cloudfoundry.servlet;

import org.springframework.boot.actuate.autoconfigure.cloudfoundry.AccessLevel;
import org.springframework.boot.actuate.autoconfigure.cloudfoundry.SecurityResponse;
import org.springframework.boot.actuate.endpoint.EndpointId;
import org.springframework.boot.actuate.endpoint.web.*;
import org.springframework.boot.actuate.endpoint.web.servlet.AbstractWebMvcEndpointHandlerMapping;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.servlet.mvc.method.RequestMappingInfoHandlerMapping;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * A custom {@link RequestMappingInfoHandlerMapping} that makes web endpoints available on
 * Cloud Foundry specific URLs over HTTP using Spring MVC.
 *
 * @author Madhura Bhave
 * @author Phillip Webb
 * @author Brian Clozel
 */
class CloudFoundryWebEndpointServletHandlerMapping extends AbstractWebMvcEndpointHandlerMapping {

	private final CloudFoundrySecurityInterceptor securityInterceptor;

	private final EndpointLinksResolver linksResolver;

	CloudFoundryWebEndpointServletHandlerMapping(EndpointMapping endpointMapping,
												 Collection<ExposableWebEndpoint> endpoints, EndpointMediaTypes endpointMediaTypes,
												 CorsConfiguration corsConfiguration, CloudFoundrySecurityInterceptor securityInterceptor,
												 EndpointLinksResolver linksResolver) {
		super(endpointMapping, endpoints, endpointMediaTypes, corsConfiguration);
		this.securityInterceptor = securityInterceptor;
		this.linksResolver = linksResolver;
	}

	@Override
	protected ServletWebOperation wrapServletWebOperation(ExposableWebEndpoint endpoint, WebOperation operation,
														  ServletWebOperation servletWebOperation) {
		return new SecureServletWebOperation(servletWebOperation, this.securityInterceptor, endpoint.getEndpointId());
	}

	@Override
	protected LinksHandler getLinksHandler() {
		return new CloudFoundryLinksHandler();
	}

	/**
	 * {@link ServletWebOperation} wrapper to add security.
	 */
	private static class SecureServletWebOperation implements ServletWebOperation {

		private final ServletWebOperation delegate;

		private final CloudFoundrySecurityInterceptor securityInterceptor;

		private final EndpointId endpointId;

		SecureServletWebOperation(ServletWebOperation delegate, CloudFoundrySecurityInterceptor securityInterceptor,
								  EndpointId endpointId) {
			this.delegate = delegate;
			this.securityInterceptor = securityInterceptor;
			this.endpointId = endpointId;
		}

		@Override
		public Object handle(HttpServletRequest request, Map<String, String> body) {
			SecurityResponse securityResponse = this.securityInterceptor.preHandle(request, this.endpointId);
			if (!securityResponse.getStatus().equals(HttpStatus.OK)) {
				return new ResponseEntity<Object>(securityResponse.getMessage(), securityResponse.getStatus());
			}
			return this.delegate.handle(request, body);
		}

	}

	class CloudFoundryLinksHandler implements LinksHandler {

		@Override
		@ResponseBody
		public Map<String, Map<String, Link>> links(HttpServletRequest request, HttpServletResponse response) {
			SecurityResponse securityResponse = CloudFoundryWebEndpointServletHandlerMapping.this.securityInterceptor
					.preHandle(request, null);
			if (!securityResponse.getStatus().equals(HttpStatus.OK)) {
				sendFailureResponse(response, securityResponse);
			}
			AccessLevel accessLevel = (AccessLevel) request.getAttribute(AccessLevel.REQUEST_ATTRIBUTE);
			Map<String, Link> links = CloudFoundryWebEndpointServletHandlerMapping.this.linksResolver
					.resolveLinks(request.getRequestURL().toString());
			Map<String, Link> filteredLinks = new LinkedHashMap<>();
			if (accessLevel == null) {
				return Collections.singletonMap("_links", filteredLinks);
			}
			filteredLinks = links.entrySet().stream()
					.filter((e) -> e.getKey().equals("self") || accessLevel.isAccessAllowed(e.getKey()))
					.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
			return Collections.singletonMap("_links", filteredLinks);
		}

		@Override
		public String toString() {
			return "Actuator root web endpoint";
		}

		private void sendFailureResponse(HttpServletResponse response, SecurityResponse securityResponse) {
			try {
				response.sendError(securityResponse.getStatus().value(), securityResponse.getMessage());
			} catch (Exception ex) {
				logger.debug("Failed to send error response", ex);
			}
		}

	}

}
