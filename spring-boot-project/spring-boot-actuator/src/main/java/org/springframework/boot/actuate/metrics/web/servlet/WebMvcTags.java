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

package org.springframework.boot.actuate.metrics.web.servlet;

import io.micrometer.core.instrument.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.util.pattern.PathPattern;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.regex.Pattern;

/**
 * Factory methods for {@link Tag Tags} associated with a request-response exchange that
 * is handled by Spring MVC.
 *
 * @author Jon Schneider
 * @author Andy Wilkinson
 * @author Brian Clozel
 * @author Michael McFadyen
 * @since 2.0.0
 */
public final class WebMvcTags {

	private static final String DATA_REST_PATH_PATTERN_ATTRIBUTE = "org.springframework.data.rest.webmvc.RepositoryRestHandlerMapping.EFFECTIVE_REPOSITORY_RESOURCE_LOOKUP_PATH";

	private static final Tag URI_NOT_FOUND = Tag.of("uri", "NOT_FOUND");

	private static final Tag URI_REDIRECTION = Tag.of("uri", "REDIRECTION");

	private static final Tag URI_ROOT = Tag.of("uri", "root");

	private static final Tag URI_UNKNOWN = Tag.of("uri", "UNKNOWN");

	private static final Tag EXCEPTION_NONE = Tag.of("exception", "None");

	private static final Tag STATUS_UNKNOWN = Tag.of("status", "UNKNOWN");

	private static final Tag OUTCOME_UNKNOWN = Tag.of("outcome", "UNKNOWN");

	private static final Tag OUTCOME_INFORMATIONAL = Tag.of("outcome", "INFORMATIONAL");

	private static final Tag OUTCOME_SUCCESS = Tag.of("outcome", "SUCCESS");

	private static final Tag OUTCOME_REDIRECTION = Tag.of("outcome", "REDIRECTION");

	private static final Tag OUTCOME_CLIENT_ERROR = Tag.of("outcome", "CLIENT_ERROR");

	private static final Tag OUTCOME_SERVER_ERROR = Tag.of("outcome", "SERVER_ERROR");

	private static final Tag METHOD_UNKNOWN = Tag.of("method", "UNKNOWN");

	private static final Pattern TRAILING_SLASH_PATTERN = Pattern.compile("/$");

	private static final Pattern MULTIPLE_SLASH_PATTERN = Pattern.compile("//+");

	private WebMvcTags() {
	}

	/**
	 * Creates a {@code method} tag based on the {@link HttpServletRequest#getMethod()
	 * method} of the given {@code request}.
	 *
	 * @param request the request
	 * @return the method tag whose value is a capitalized method (e.g. GET).
	 */
	public static Tag method(HttpServletRequest request) {
		return (request != null) ? Tag.of("method", request.getMethod()) : METHOD_UNKNOWN;
	}

	/**
	 * Creates a {@code status} tag based on the status of the given {@code response}.
	 *
	 * @param response the HTTP response
	 * @return the status tag derived from the status of the response
	 */
	public static Tag status(HttpServletResponse response) {
		return (response != null) ? Tag.of("status", Integer.toString(response.getStatus())) : STATUS_UNKNOWN;
	}

	/**
	 * Creates a {@code uri} tag based on the URI of the given {@code request}. Uses the
	 * {@link HandlerMapping#BEST_MATCHING_PATTERN_ATTRIBUTE} best matching pattern if
	 * available. Falling back to {@code REDIRECTION} for 3xx responses, {@code NOT_FOUND}
	 * for 404 responses, {@code root} for requests with no path info, and {@code UNKNOWN}
	 * for all other requests.
	 *
	 * @param request  the request
	 * @param response the response
	 * @return the uri tag derived from the request
	 */
	public static Tag uri(HttpServletRequest request, HttpServletResponse response) {
		if (request != null) {
			String pattern = getMatchingPattern(request);
			if (pattern != null) {
				return Tag.of("uri", pattern);
			}
			if (response != null) {
				HttpStatus status = extractStatus(response);
				if (status != null) {
					if (status.is3xxRedirection()) {
						return URI_REDIRECTION;
					}
					if (status == HttpStatus.NOT_FOUND) {
						return URI_NOT_FOUND;
					}
				}
			}
			String pathInfo = getPathInfo(request);
			if (pathInfo.isEmpty()) {
				return URI_ROOT;
			}
		}
		return URI_UNKNOWN;
	}

	private static HttpStatus extractStatus(HttpServletResponse response) {
		try {
			return HttpStatus.valueOf(response.getStatus());
		} catch (IllegalArgumentException ex) {
			return null;
		}
	}

	private static String getMatchingPattern(HttpServletRequest request) {
		PathPattern dataRestPathPattern = (PathPattern) request.getAttribute(DATA_REST_PATH_PATTERN_ATTRIBUTE);
		if (dataRestPathPattern != null) {
			return dataRestPathPattern.getPatternString();
		}
		return (String) request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
	}

	private static String getPathInfo(HttpServletRequest request) {
		String pathInfo = request.getPathInfo();
		String uri = StringUtils.hasText(pathInfo) ? pathInfo : "/";
		uri = MULTIPLE_SLASH_PATTERN.matcher(uri).replaceAll("/");
		return TRAILING_SLASH_PATTERN.matcher(uri).replaceAll("");
	}

	/**
	 * Creates a {@code exception} tag based on the {@link Class#getSimpleName() simple
	 * name} of the class of the given {@code exception}.
	 *
	 * @param exception the exception, may be {@code null}
	 * @return the exception tag derived from the exception
	 */
	public static Tag exception(Throwable exception) {
		if (exception != null) {
			String simpleName = exception.getClass().getSimpleName();
			return Tag.of("exception", StringUtils.hasText(simpleName) ? simpleName : exception.getClass().getName());
		}
		return EXCEPTION_NONE;
	}

	/**
	 * Creates an {@code outcome} tag based on the status of the given {@code response}.
	 *
	 * @param response the HTTP response
	 * @return the outcome tag derived from the status of the response
	 * @since 2.1.0
	 */
	public static Tag outcome(HttpServletResponse response) {
		if (response != null) {
			int status = response.getStatus();
			if (status < 200) {
				return OUTCOME_INFORMATIONAL;
			}
			if (status < 300) {
				return OUTCOME_SUCCESS;
			}
			if (status < 400) {
				return OUTCOME_REDIRECTION;
			}
			if (status < 500) {
				return OUTCOME_CLIENT_ERROR;
			}
			return OUTCOME_SERVER_ERROR;
		}
		return OUTCOME_UNKNOWN;
	}

}
