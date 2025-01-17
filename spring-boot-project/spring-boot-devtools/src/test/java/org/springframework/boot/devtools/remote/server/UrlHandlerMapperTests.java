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

package org.springframework.boot.devtools.remote.server;

import org.junit.Test;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.mock.web.MockHttpServletRequest;

import javax.servlet.http.HttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link UrlHandlerMapper}.
 *
 * @author Rob Winch
 * @author Phillip Webb
 */
public class UrlHandlerMapperTests {

	private Handler handler = mock(Handler.class);

	@Test
	public void requestUriMustNotBeNull() {
		assertThatIllegalArgumentException().isThrownBy(() -> new UrlHandlerMapper(null, this.handler))
				.withMessageContaining("URL must not be empty");
	}

	@Test
	public void requestUriMustNotBeEmpty() {
		assertThatIllegalArgumentException().isThrownBy(() -> new UrlHandlerMapper("", this.handler))
				.withMessageContaining("URL must not be empty");
	}

	@Test
	public void requestUrlMustStartWithSlash() {
		assertThatIllegalArgumentException().isThrownBy(() -> new UrlHandlerMapper("tunnel", this.handler))
				.withMessageContaining("URL must start with '/'");
	}

	@Test
	public void handlesMatchedUrl() {
		UrlHandlerMapper mapper = new UrlHandlerMapper("/tunnel", this.handler);
		HttpServletRequest servletRequest = new MockHttpServletRequest("GET", "/tunnel");
		ServerHttpRequest request = new ServletServerHttpRequest(servletRequest);
		assertThat(mapper.getHandler(request)).isEqualTo(this.handler);
	}

	@Test
	public void ignoresDifferentUrl() {
		UrlHandlerMapper mapper = new UrlHandlerMapper("/tunnel", this.handler);
		HttpServletRequest servletRequest = new MockHttpServletRequest("GET", "/tunnel/other");
		ServerHttpRequest request = new ServletServerHttpRequest(servletRequest);
		assertThat(mapper.getHandler(request)).isNull();
	}

}
