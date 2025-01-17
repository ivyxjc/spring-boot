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

package org.springframework.boot.test.autoconfigure;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.junit4.SpringRunner;

import javax.annotation.PostConstruct;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link SpringBootDependencyInjectionTestExecutionListener}.
 *
 * @author Phillip Webb
 */
@RunWith(SpringRunner.class)
@SpringBootTest
public class SpringBootDependencyInjectionTestExecutionListenerPostConstructIntegrationTests {

	private List<String> calls = new ArrayList<>();

	@PostConstruct
	public void postConstruct() {
		StringWriter writer = new StringWriter();
		new RuntimeException().printStackTrace(new PrintWriter(writer));
		this.calls.add(writer.toString());
	}

	@Test
	public void postConstructShouldBeInvokedOnlyOnce() {
		// gh-6874
		assertThat(this.calls).hasSize(1);
	}

	@Configuration
	static class Config {

	}

}
