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

package org.springframework.boot.gradle.tasks.bundling;

import org.gradle.testkit.runner.InvalidRunnerConfigurationException;
import org.gradle.testkit.runner.TaskOutcome;
import org.gradle.testkit.runner.UnexpectedBuildFailure;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.gradle.junit.GradleCompatibilitySuite;
import org.springframework.boot.gradle.testkit.GradleBuild;
import org.springframework.boot.loader.tools.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.jar.JarFile;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link BootJar}.
 *
 * @author Andy Wilkinson
 */
@RunWith(GradleCompatibilitySuite.class)
public abstract class AbstractBootArchiveIntegrationTests {

	private final String taskName;
	@Rule
	public GradleBuild gradleBuild;

	protected AbstractBootArchiveIntegrationTests(String taskName) {
		this.taskName = taskName;
	}

	@Test
	public void basicBuild() throws InvalidRunnerConfigurationException, UnexpectedBuildFailure, IOException {
		assertThat(this.gradleBuild.build(this.taskName).task(":" + this.taskName).getOutcome())
				.isEqualTo(TaskOutcome.SUCCESS);
	}

	@Test
	public void reproducibleArchive()
			throws InvalidRunnerConfigurationException, UnexpectedBuildFailure, IOException, InterruptedException {
		assertThat(this.gradleBuild.build(this.taskName).task(":" + this.taskName).getOutcome())
				.isEqualTo(TaskOutcome.SUCCESS);
		File jar = new File(this.gradleBuild.getProjectDir(), "build/libs").listFiles()[0];
		String firstHash = FileUtils.sha1Hash(jar);
		Thread.sleep(1500);
		assertThat(this.gradleBuild.build("clean", this.taskName).task(":" + this.taskName).getOutcome())
				.isEqualTo(TaskOutcome.SUCCESS);
		String secondHash = FileUtils.sha1Hash(jar);
		assertThat(firstHash).isEqualTo(secondHash);
	}

	@Test
	public void upToDateWhenBuiltTwice()
			throws InvalidRunnerConfigurationException, UnexpectedBuildFailure, IOException {
		assertThat(this.gradleBuild.build(this.taskName).task(":" + this.taskName).getOutcome())
				.isEqualTo(TaskOutcome.SUCCESS);
		assertThat(this.gradleBuild.build(this.taskName).task(":" + this.taskName).getOutcome())
				.isEqualTo(TaskOutcome.UP_TO_DATE);
	}

	@Test
	public void upToDateWhenBuiltTwiceWithLaunchScriptIncluded()
			throws InvalidRunnerConfigurationException, UnexpectedBuildFailure, IOException {
		assertThat(this.gradleBuild.build("-PincludeLaunchScript=true", this.taskName).task(":" + this.taskName)
				.getOutcome()).isEqualTo(TaskOutcome.SUCCESS);
		assertThat(this.gradleBuild.build("-PincludeLaunchScript=true", this.taskName).task(":" + this.taskName)
				.getOutcome()).isEqualTo(TaskOutcome.UP_TO_DATE);
	}

	@Test
	public void notUpToDateWhenLaunchScriptWasNotIncludedAndThenIsIncluded() {
		assertThat(this.gradleBuild.build(this.taskName).task(":" + this.taskName).getOutcome())
				.isEqualTo(TaskOutcome.SUCCESS);
		assertThat(this.gradleBuild.build("-PincludeLaunchScript=true", this.taskName).task(":" + this.taskName)
				.getOutcome()).isEqualTo(TaskOutcome.SUCCESS);
	}

	@Test
	public void notUpToDateWhenLaunchScriptWasIncludedAndThenIsNotIncluded() {
		assertThat(this.gradleBuild.build(this.taskName).task(":" + this.taskName).getOutcome())
				.isEqualTo(TaskOutcome.SUCCESS);
		assertThat(this.gradleBuild.build("-PincludeLaunchScript=true", this.taskName).task(":" + this.taskName)
				.getOutcome()).isEqualTo(TaskOutcome.SUCCESS);
	}

	@Test
	public void notUpToDateWhenLaunchScriptPropertyChanges() {
		assertThat(this.gradleBuild.build("-PincludeLaunchScript=true", "-PlaunchScriptProperty=foo", this.taskName)
				.task(":" + this.taskName).getOutcome()).isEqualTo(TaskOutcome.SUCCESS);
		assertThat(this.gradleBuild.build("-PincludeLaunchScript=true", "-PlaunchScriptProperty=bar", this.taskName)
				.task(":" + this.taskName).getOutcome()).isEqualTo(TaskOutcome.SUCCESS);
	}

	@Test
	public void applicationPluginMainClassNameIsUsed() throws IOException {
		assertThat(this.gradleBuild.build(this.taskName).task(":" + this.taskName).getOutcome())
				.isEqualTo(TaskOutcome.SUCCESS);
		try (JarFile jarFile = new JarFile(new File(this.gradleBuild.getProjectDir(), "build/libs").listFiles()[0])) {
			assertThat(jarFile.getManifest().getMainAttributes().getValue("Start-Class"))
					.isEqualTo("com.example.CustomMain");
		}
	}

	@Test
	public void springBootExtensionMainClassNameIsUsed() throws IOException {
		assertThat(this.gradleBuild.build(this.taskName).task(":" + this.taskName).getOutcome())
				.isEqualTo(TaskOutcome.SUCCESS);
		try (JarFile jarFile = new JarFile(new File(this.gradleBuild.getProjectDir(), "build/libs").listFiles()[0])) {
			assertThat(jarFile.getManifest().getMainAttributes().getValue("Start-Class"))
					.isEqualTo("com.example.CustomMain");
		}
	}

	@Test
	public void duplicatesAreHandledGracefully() throws IOException {
		assertThat(this.gradleBuild.build(this.taskName).task(":" + this.taskName).getOutcome())
				.isEqualTo(TaskOutcome.SUCCESS);
	}

}
