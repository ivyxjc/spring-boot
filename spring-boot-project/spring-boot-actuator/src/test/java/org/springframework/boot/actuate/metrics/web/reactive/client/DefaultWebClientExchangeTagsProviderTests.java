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

package org.springframework.boot.actuate.metrics.web.reactive.client;

import io.micrometer.core.instrument.Tag;
import org.junit.Before;
import org.junit.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.IOException;
import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link DefaultWebClientExchangeTagsProvider}
 *
 * @author Brian Clozel
 */
public class DefaultWebClientExchangeTagsProviderTests {

	private static final String URI_TEMPLATE_ATTRIBUTE = WebClient.class.getName() + ".uriTemplate";

	private WebClientExchangeTagsProvider tagsProvider = new DefaultWebClientExchangeTagsProvider();

	private ClientRequest request;

	private ClientResponse response;

	@Before
	public void setup() {
		this.request = ClientRequest.create(HttpMethod.GET, URI.create("https://example.org/projects/spring-boot"))
				.attribute(URI_TEMPLATE_ATTRIBUTE, "https://example.org/projects/{project}").build();
		this.response = mock(ClientResponse.class);
		given(this.response.rawStatusCode()).willReturn(HttpStatus.OK.value());
	}

	@Test
	public void tagsShouldBePopulated() {
		Iterable<Tag> tags = this.tagsProvider.tags(this.request, this.response, null);
		assertThat(tags).containsExactlyInAnyOrder(Tag.of("method", "GET"), Tag.of("uri", "/projects/{project}"),
				Tag.of("clientName", "example.org"), Tag.of("status", "200"));
	}

	@Test
	public void tagsWhenNoUriTemplateShouldProvideUriPath() {
		ClientRequest request = ClientRequest
				.create(HttpMethod.GET, URI.create("https://example.org/projects/spring-boot")).build();
		Iterable<Tag> tags = this.tagsProvider.tags(request, this.response, null);
		assertThat(tags).containsExactlyInAnyOrder(Tag.of("method", "GET"), Tag.of("uri", "/projects/spring-boot"),
				Tag.of("clientName", "example.org"), Tag.of("status", "200"));
	}

	@Test
	public void tagsWhenIoExceptionShouldReturnIoErrorStatus() {
		Iterable<Tag> tags = this.tagsProvider.tags(this.request, null, new IOException());
		assertThat(tags).containsExactlyInAnyOrder(Tag.of("method", "GET"), Tag.of("uri", "/projects/{project}"),
				Tag.of("clientName", "example.org"), Tag.of("status", "IO_ERROR"));
	}

	@Test
	public void tagsWhenExceptionShouldReturnClientErrorStatus() {
		Iterable<Tag> tags = this.tagsProvider.tags(this.request, null, new IllegalArgumentException());
		assertThat(tags).containsExactlyInAnyOrder(Tag.of("method", "GET"), Tag.of("uri", "/projects/{project}"),
				Tag.of("clientName", "example.org"), Tag.of("status", "CLIENT_ERROR"));
	}

}
