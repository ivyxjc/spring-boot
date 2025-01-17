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

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.boot.autoconfigure.freemarker.FreeMarkerAutoConfiguration;
import org.springframework.boot.autoconfigure.thymeleaf.ThymeleafAutoConfiguration;
import org.springframework.core.annotation.AliasFor;
import org.springframework.core.env.Environment;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.classreading.SimpleMetadataReaderFactory;
import org.springframework.util.ClassUtils;

import java.io.IOException;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Arrays;
import java.util.Collection;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyZeroInteractions;

/**
 * Tests for {@link ImportAutoConfigurationImportSelector}.
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 */
public class ImportAutoConfigurationImportSelectorTests {

	private final ImportAutoConfigurationImportSelector importSelector = new TestImportAutoConfigurationImportSelector();

	private final ConfigurableListableBeanFactory beanFactory = new DefaultListableBeanFactory();

	@Mock
	private Environment environment;

	@Before
	public void setup() {
		MockitoAnnotations.initMocks(this);
		this.importSelector.setBeanFactory(this.beanFactory);
		this.importSelector.setEnvironment(this.environment);
		this.importSelector.setResourceLoader(new DefaultResourceLoader());
	}

	@Test
	public void importsAreSelected() throws Exception {
		AnnotationMetadata annotationMetadata = getAnnotationMetadata(ImportFreeMarker.class);
		String[] imports = this.importSelector.selectImports(annotationMetadata);
		assertThat(imports).containsExactly(FreeMarkerAutoConfiguration.class.getName());
	}

	@Test
	public void importsAreSelectedUsingClassesAttribute() throws Exception {
		AnnotationMetadata annotationMetadata = getAnnotationMetadata(ImportFreeMarkerUsingClassesAttribute.class);
		String[] imports = this.importSelector.selectImports(annotationMetadata);
		assertThat(imports).containsExactly(FreeMarkerAutoConfiguration.class.getName());
	}

	@Test
	public void propertyExclusionsAreNotApplied() throws Exception {
		AnnotationMetadata annotationMetadata = getAnnotationMetadata(ImportFreeMarker.class);
		this.importSelector.selectImports(annotationMetadata);
		verifyZeroInteractions(this.environment);
	}

	@Test
	public void multipleImportsAreFound() throws Exception {
		AnnotationMetadata annotationMetadata = getAnnotationMetadata(MultipleImports.class);
		String[] imports = this.importSelector.selectImports(annotationMetadata);
		assertThat(imports).containsOnly(FreeMarkerAutoConfiguration.class.getName(),
				ThymeleafAutoConfiguration.class.getName());
	}

	@Test
	public void selfAnnotatingAnnotationDoesNotCauseStackOverflow() throws IOException {
		AnnotationMetadata annotationMetadata = getAnnotationMetadata(ImportWithSelfAnnotatingAnnotation.class);
		String[] imports = this.importSelector.selectImports(annotationMetadata);
		assertThat(imports).containsOnly(ThymeleafAutoConfiguration.class.getName());
	}

	@Test
	public void exclusionsAreApplied() throws Exception {
		AnnotationMetadata annotationMetadata = getAnnotationMetadata(MultipleImportsWithExclusion.class);
		String[] imports = this.importSelector.selectImports(annotationMetadata);
		assertThat(imports).containsOnly(FreeMarkerAutoConfiguration.class.getName());
	}

	@Test
	public void exclusionsWithoutImport() throws Exception {
		AnnotationMetadata annotationMetadata = getAnnotationMetadata(ExclusionWithoutImport.class);
		String[] imports = this.importSelector.selectImports(annotationMetadata);
		assertThat(imports).containsOnly(FreeMarkerAutoConfiguration.class.getName());
	}

	@Test
	public void exclusionsAliasesAreApplied() throws Exception {
		AnnotationMetadata annotationMetadata = getAnnotationMetadata(ImportWithSelfAnnotatingAnnotationExclude.class);
		String[] imports = this.importSelector.selectImports(annotationMetadata);
		assertThat(imports).isEmpty();
	}

	@Test
	public void determineImportsWhenUsingMetaWithoutClassesShouldBeEqual() throws Exception {
		Set<Object> set1 = this.importSelector
				.determineImports(getAnnotationMetadata(ImportMetaAutoConfigurationWithUnrelatedOne.class));
		Set<Object> set2 = this.importSelector
				.determineImports(getAnnotationMetadata(ImportMetaAutoConfigurationWithUnrelatedTwo.class));
		assertThat(set1).isEqualTo(set2);
		assertThat(set1.hashCode()).isEqualTo(set2.hashCode());
	}

	@Test
	public void determineImportsWhenUsingNonMetaWithoutClassesShouldBeSame() throws Exception {
		Set<Object> set1 = this.importSelector
				.determineImports(getAnnotationMetadata(ImportAutoConfigurationWithUnrelatedOne.class));
		Set<Object> set2 = this.importSelector
				.determineImports(getAnnotationMetadata(ImportAutoConfigurationWithUnrelatedTwo.class));
		assertThat(set1).isEqualTo(set2);
	}

	@Test
	public void determineImportsWhenUsingNonMetaWithClassesShouldBeSame() throws Exception {
		Set<Object> set1 = this.importSelector
				.determineImports(getAnnotationMetadata(ImportAutoConfigurationWithItemsOne.class));
		Set<Object> set2 = this.importSelector
				.determineImports(getAnnotationMetadata(ImportAutoConfigurationWithItemsTwo.class));
		assertThat(set1).isEqualTo(set2);
	}

	@Test
	public void determineImportsWhenUsingMetaExcludeWithoutClassesShouldBeEqual() throws Exception {
		Set<Object> set1 = this.importSelector
				.determineImports(getAnnotationMetadata(ImportMetaAutoConfigurationExcludeWithUnrelatedOne.class));
		Set<Object> set2 = this.importSelector
				.determineImports(getAnnotationMetadata(ImportMetaAutoConfigurationExcludeWithUnrelatedTwo.class));
		assertThat(set1).isEqualTo(set2);
		assertThat(set1.hashCode()).isEqualTo(set2.hashCode());
	}

	@Test
	public void determineImportsWhenUsingMetaDifferentExcludeWithoutClassesShouldBeDifferent() throws Exception {
		Set<Object> set1 = this.importSelector
				.determineImports(getAnnotationMetadata(ImportMetaAutoConfigurationExcludeWithUnrelatedOne.class));
		Set<Object> set2 = this.importSelector
				.determineImports(getAnnotationMetadata(ImportMetaAutoConfigurationWithUnrelatedTwo.class));
		assertThat(set1).isNotEqualTo(set2);
	}

	@Test
	public void determineImportsShouldNotSetPackageImport() throws Exception {
		Class<?> packageImportClass = ClassUtils.resolveClassName(
				"org.springframework.boot.autoconfigure.AutoConfigurationPackages.PackageImport", null);
		Set<Object> selectedImports = this.importSelector
				.determineImports(getAnnotationMetadata(ImportMetaAutoConfigurationExcludeWithUnrelatedOne.class));
		for (Object selectedImport : selectedImports) {
			assertThat(selectedImport).isNotInstanceOf(packageImportClass);
		}
	}

	private AnnotationMetadata getAnnotationMetadata(Class<?> source) throws IOException {
		return new SimpleMetadataReaderFactory().getMetadataReader(source.getName()).getAnnotationMetadata();
	}

	@Retention(RetentionPolicy.RUNTIME)
	@ImportAutoConfiguration(FreeMarkerAutoConfiguration.class)
	@interface ImportOne {

	}

	@Retention(RetentionPolicy.RUNTIME)
	@ImportAutoConfiguration(ThymeleafAutoConfiguration.class)
	@interface ImportTwo {

	}

	@ImportAutoConfiguration
	@Retention(RetentionPolicy.RUNTIME)
	@interface MetaImportAutoConfiguration {

		@AliasFor(annotation = ImportAutoConfiguration.class)
		Class<?>[] exclude() default {};

	}

	@Retention(RetentionPolicy.RUNTIME)
	@interface UnrelatedOne {

	}

	@Retention(RetentionPolicy.RUNTIME)
	@interface UnrelatedTwo {

	}

	@Retention(RetentionPolicy.RUNTIME)
	@ImportAutoConfiguration(ThymeleafAutoConfiguration.class)
	@SelfAnnotating
	@interface SelfAnnotating {

		@AliasFor(annotation = ImportAutoConfiguration.class, attribute = "exclude")
		Class<?>[] excludeAutoConfiguration() default {};

	}

	@ImportAutoConfiguration(FreeMarkerAutoConfiguration.class)
	static class ImportFreeMarker {

	}

	@ImportAutoConfiguration(classes = FreeMarkerAutoConfiguration.class)
	static class ImportFreeMarkerUsingClassesAttribute {

	}

	@ImportOne
	@ImportTwo
	static class MultipleImports {

	}

	@ImportOne
	@ImportTwo
	@ImportAutoConfiguration(exclude = ThymeleafAutoConfiguration.class)
	static class MultipleImportsWithExclusion {

	}

	@ImportOne
	@ImportAutoConfiguration(exclude = ThymeleafAutoConfiguration.class)
	static class ExclusionWithoutImport {

	}

	@SelfAnnotating
	static class ImportWithSelfAnnotatingAnnotation {

	}

	@SelfAnnotating(excludeAutoConfiguration = ThymeleafAutoConfiguration.class)
	static class ImportWithSelfAnnotatingAnnotationExclude {

	}

	@MetaImportAutoConfiguration
	@UnrelatedOne
	static class ImportMetaAutoConfigurationWithUnrelatedOne {

	}

	@MetaImportAutoConfiguration
	@UnrelatedTwo
	static class ImportMetaAutoConfigurationWithUnrelatedTwo {

	}

	@ImportAutoConfiguration
	@UnrelatedOne
	static class ImportAutoConfigurationWithUnrelatedOne {

	}

	@ImportAutoConfiguration
	@UnrelatedTwo
	static class ImportAutoConfigurationWithUnrelatedTwo {

	}

	@ImportAutoConfiguration(classes = ThymeleafAutoConfiguration.class)
	@UnrelatedOne
	static class ImportAutoConfigurationWithItemsOne {

	}

	@ImportAutoConfiguration(classes = ThymeleafAutoConfiguration.class)
	@UnrelatedTwo
	static class ImportAutoConfigurationWithItemsTwo {

	}

	@MetaImportAutoConfiguration(exclude = ThymeleafAutoConfiguration.class)
	@UnrelatedOne
	static class ImportMetaAutoConfigurationExcludeWithUnrelatedOne {

	}

	@MetaImportAutoConfiguration(exclude = ThymeleafAutoConfiguration.class)
	@UnrelatedTwo
	static class ImportMetaAutoConfigurationExcludeWithUnrelatedTwo {

	}

	private static class TestImportAutoConfigurationImportSelector extends ImportAutoConfigurationImportSelector {

		@Override
		protected Collection<String> loadFactoryNames(Class<?> source) {
			if (source == MetaImportAutoConfiguration.class) {
				return Arrays.asList(ThymeleafAutoConfiguration.class.getName(),
						FreeMarkerAutoConfiguration.class.getName());
			}
			return super.loadFactoryNames(source);
		}

	}

}
