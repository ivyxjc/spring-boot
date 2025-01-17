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

package org.springframework.boot.actuate.web.trace.reactive;

import org.springframework.boot.actuate.trace.http.TraceableResponse;
import org.springframework.http.server.reactive.ServerHttpResponse;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * An adapter that exposes a {@link ServerHttpResponse} as a {@link TraceableResponse}.
 *
 * @author Andy Wilkinson
 */
class TraceableServerHttpResponse implements TraceableResponse {

	private final int status;

	private final Map<String, List<String>> headers;

	TraceableServerHttpResponse(ServerHttpResponse response) {
		this.status = (response.getStatusCode() != null) ? response.getStatusCode().value() : 200;
		this.headers = new LinkedHashMap<>(response.getHeaders());

	}

	@Override
	public int getStatus() {
		return this.status;
	}

	@Override
	public Map<String, List<String>> getHeaders() {
		return this.headers;
	}

}
