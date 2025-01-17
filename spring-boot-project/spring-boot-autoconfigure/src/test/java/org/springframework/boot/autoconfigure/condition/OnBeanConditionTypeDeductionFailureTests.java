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

package org.springframework.boot.autoconfigure.condition;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.autoconfigure.condition.OnBeanCondition.BeanTypeDeductionException;
import org.springframework.boot.testsupport.runner.classpath.ClassPathExclusions;
import org.springframework.boot.testsupport.runner.classpath.ModifiedClassPathRunner;
import org.springframework.context.annotation.*;
import org.springframework.core.type.AnnotationMetadata;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Tests for {@link OnBeanCondition} when deduction of the bean's type fails
 *
 * @author Andy Wilkinson
 */
@RunWith(ModifiedClassPathRunner.class)
@ClassPathExclusions("jackson-core-*.jar")
public class OnBeanConditionTypeDeductionFailureTests {

	@Test
	public void conditionalOnMissingBeanWithDeducedTypeThatIsPartiallyMissingFromClassPath() {
		assertThatExceptionOfType(Exception.class)
				.isThrownBy(() -> new AnnotationConfigApplicationContext(ImportingConfiguration.class).close())
				.satisfies((ex) -> {
					Throwable beanTypeDeductionException = findNestedCause(ex, BeanTypeDeductionException.class);
					assertThat(beanTypeDeductionException).hasMessage("Failed to deduce bean type for "
							+ OnMissingBeanConfiguration.class.getName() + ".objectMapper");
					assertThat(findNestedCause(beanTypeDeductionException, NoClassDefFoundError.class)).isNotNull();

				});
	}

	private Throwable findNestedCause(Throwable ex, Class<? extends Throwable> target) {
		Throwable candidate = ex;
		while (candidate != null) {
			if (target.isInstance(candidate)) {
				return candidate;
			}
			candidate = candidate.getCause();
		}
		return null;
	}

	@Configuration
	@Import(OnMissingBeanImportSelector.class)
	static class ImportingConfiguration {

	}

	@Configuration
	static class OnMissingBeanConfiguration {

		@Bean
		@ConditionalOnMissingBean
		public ObjectMapper objectMapper() {
			return new ObjectMapper();
		}

	}

	static class OnMissingBeanImportSelector implements ImportSelector {

		@Override
		public String[] selectImports(AnnotationMetadata importingClassMetadata) {
			return new String[]{OnMissingBeanConfiguration.class.getName()};
		}

	}

}
