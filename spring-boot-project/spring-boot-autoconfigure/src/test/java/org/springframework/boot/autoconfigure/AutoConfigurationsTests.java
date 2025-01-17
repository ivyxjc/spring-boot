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

package org.springframework.boot.autoconfigure;

import org.junit.Test;
import org.springframework.boot.context.annotation.Configurations;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link AutoConfigurations}.
 *
 * @author Phillip Webb
 */
public class AutoConfigurationsTests {

	@Test
	public void ofShouldCreateOrderedConfigurations() {
		Configurations configurations = AutoConfigurations.of(AutoConfigureA.class, AutoConfigureB.class);
		assertThat(Configurations.getClasses(configurations)).containsExactly(AutoConfigureB.class,
				AutoConfigureA.class);
	}

	@AutoConfigureAfter(AutoConfigureB.class)
	public static class AutoConfigureA {

	}

	public static class AutoConfigureB {

	}

}
