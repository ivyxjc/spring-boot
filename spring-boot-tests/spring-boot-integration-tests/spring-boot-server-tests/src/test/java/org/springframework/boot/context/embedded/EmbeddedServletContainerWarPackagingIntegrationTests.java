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

package org.springframework.boot.context.embedded;

import org.junit.Assume;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for Spring Boot's embedded servlet container support using war
 * packaging.
 *
 * @author Andy Wilkinson
 */
@RunWith(Parameterized.class)
public class EmbeddedServletContainerWarPackagingIntegrationTests
		extends AbstractEmbeddedServletContainerIntegrationTests {

	public EmbeddedServletContainerWarPackagingIntegrationTests(String name, AbstractApplicationLauncher launcher) {
		super(name, launcher);
	}

	@Parameters(name = "{0}")
	public static Object[] parameters() {
		return AbstractEmbeddedServletContainerIntegrationTests.parameters("war",
				Arrays.asList(PackagedApplicationLauncher.class, ExplodedApplicationLauncher.class));
	}

	@Test
	public void nestedMetaInfResourceIsAvailableViaHttp() {
		ResponseEntity<String> entity = this.rest.getForEntity("/nested-meta-inf-resource.txt", String.class);
		assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.OK);
	}

	@Test
	public void nestedMetaInfResourceWithNameThatContainsReservedCharactersIsAvailableViaHttp() {
		Assume.assumeFalse(isWindows());
		ResponseEntity<String> entity = this.rest.getForEntity(
				"/nested-reserved-%21%23%24%25%26%28%29%2A%2B%2C%3A%3D%3F%40%5B%5D-meta-inf-resource.txt",
				String.class);
		assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(entity.getBody()).isEqualTo("encoded-name");
	}

	@Test
	public void nestedMetaInfResourceIsAvailableViaServletContext() {
		ResponseEntity<String> entity = this.rest.getForEntity("/servletContext?/nested-meta-inf-resource.txt",
				String.class);
		assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.OK);
	}

	@Test
	public void nestedJarIsNotAvailableViaHttp() {
		ResponseEntity<String> entity = this.rest.getForEntity("/WEB-INF/lib/resources-1.0.jar", String.class);
		assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
	}

	@Test
	public void applicationClassesAreNotAvailableViaHttp() {
		ResponseEntity<String> entity = this.rest
				.getForEntity("/WEB-INF/classes/com/example/ResourceHandlingApplication.class", String.class);
		assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
	}

	@Test
	public void webappResourcesAreAvailableViaHttp() {
		ResponseEntity<String> entity = this.rest.getForEntity("/webapp-resource.txt", String.class);
		assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.OK);
	}

	@Test
	public void loaderClassesAreNotAvailableViaHttp() {
		ResponseEntity<String> entity = this.rest.getForEntity("/org/springframework/boot/loader/Launcher.class",
				String.class);
		assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
		entity = this.rest.getForEntity("/org/springframework/../springframework/boot/loader/Launcher.class",
				String.class);
		assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
	}

	@Test
	public void loaderClassesAreNotAvailableViaResourcePaths() {
		ResponseEntity<String> entity = this.rest.getForEntity("/resourcePaths", String.class);
		assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(readLines(entity.getBody()))
				.noneMatch((resourcePath) -> resourcePath.startsWith("/org/springframework/boot/loader"));
	}

	private List<String> readLines(String input) {
		if (input == null) {
			return Collections.emptyList();
		}
		try (BufferedReader reader = new BufferedReader(new StringReader(input))) {
			return reader.lines().collect(Collectors.toList());
		} catch (IOException ex) {
			throw new RuntimeException("Failed to read lines from input '" + input + "'");
		}
	}

}
