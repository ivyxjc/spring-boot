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

package org.springframework.boot.cli.command.install;

import org.assertj.core.api.Condition;
import org.junit.Before;
import org.junit.Test;
import org.springframework.boot.cli.compiler.GroovyCompilerConfiguration;
import org.springframework.boot.cli.compiler.GroovyCompilerScope;
import org.springframework.boot.cli.compiler.RepositoryConfigurationFactory;
import org.springframework.boot.cli.compiler.grape.RepositoryConfiguration;
import org.springframework.boot.testsupport.assertj.Matched;

import java.io.File;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.startsWith;

/**
 * Tests for {@link GroovyGrabDependencyResolver}.
 *
 * @author Andy Wilkinson
 */
public class GroovyGrabDependencyResolverTests {

	private DependencyResolver resolver;

	@Before
	public void setupResolver() {
		GroovyCompilerConfiguration configuration = new GroovyCompilerConfiguration() {

			@Override
			public boolean isGuessImports() {
				return true;
			}

			@Override
			public boolean isGuessDependencies() {
				return true;
			}

			@Override
			public boolean isAutoconfigure() {
				return false;
			}

			@Override
			public GroovyCompilerScope getScope() {
				return GroovyCompilerScope.DEFAULT;
			}

			@Override
			public List<RepositoryConfiguration> getRepositoryConfiguration() {
				return RepositoryConfigurationFactory.createDefaultRepositoryConfiguration();
			}

			@Override
			public String[] getClasspath() {
				return new String[]{"."};
			}

			@Override
			public boolean isQuiet() {
				return false;
			}

		};
		this.resolver = new GroovyGrabDependencyResolver(configuration);
	}

	@Test
	public void resolveArtifactWithNoDependencies() throws Exception {
		List<File> resolved = this.resolver.resolve(Arrays.asList("commons-logging:commons-logging:1.1.3"));
		assertThat(resolved).hasSize(1);
		assertThat(getNames(resolved)).containsOnly("commons-logging-1.1.3.jar");
	}

	@Test
	public void resolveArtifactWithDependencies() throws Exception {
		List<File> resolved = this.resolver.resolve(Arrays.asList("org.springframework:spring-core:4.1.1.RELEASE"));
		assertThat(resolved).hasSize(2);
		assertThat(getNames(resolved)).containsOnly("commons-logging-1.1.3.jar", "spring-core-4.1.1.RELEASE.jar");
	}

	@Test
	@SuppressWarnings({"unchecked", "rawtypes"})
	public void resolveShorthandArtifactWithDependencies() throws Exception {
		List<File> resolved = this.resolver.resolve(Arrays.asList("spring-beans"));
		assertThat(resolved).hasSize(3);
		assertThat(getNames(resolved)).has((Condition) Matched
				.by(hasItems(startsWith("spring-core-"), startsWith("spring-beans-"), startsWith("spring-jcl-"))));
	}

	@Test
	public void resolveMultipleArtifacts() throws Exception {
		List<File> resolved = this.resolver
				.resolve(Arrays.asList("junit:junit:4.11", "commons-logging:commons-logging:1.1.3"));
		assertThat(resolved).hasSize(3);
		assertThat(getNames(resolved)).containsOnly("junit-4.11.jar", "commons-logging-1.1.3.jar",
				"hamcrest-core-1.3.jar");
	}

	public Set<String> getNames(Collection<File> files) {
		Set<String> names = new HashSet<>(files.size());
		for (File file : files) {
			names.add(file.getName());
		}
		return names;
	}

}
