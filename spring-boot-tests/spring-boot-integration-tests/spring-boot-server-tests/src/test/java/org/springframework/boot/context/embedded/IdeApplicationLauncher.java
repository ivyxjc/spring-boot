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

import org.springframework.util.FileCopyUtils;
import org.springframework.util.FileSystemUtils;
import org.springframework.util.StreamUtils;
import org.springframework.util.StringUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * {@link AbstractApplicationLauncher} that launches a Spring Boot application with a
 * classpath similar to that used when run in an IDE.
 *
 * @author Andy Wilkinson
 */
class IdeApplicationLauncher extends AbstractApplicationLauncher {

	private final File exploded = new File("target/the+ide application");

	IdeApplicationLauncher(ApplicationBuilder applicationBuilder) {
		super(applicationBuilder);
	}

	@Override
	protected File getWorkingDirectory() {
		return this.exploded;
	}

	@Override
	protected String getDescription(String packaging) {
		return "IDE run " + packaging + " project";
	}

	@Override
	protected List<String> getArguments(File archive) {
		try {
			explodeArchive(archive, this.exploded);
			deleteLauncherClasses();
			File targetClasses = populateTargetClasses(archive);
			File dependencies = populateDependencies(archive);
			File resourcesProject = explodedResourcesProject(dependencies);
			if (archive.getName().endsWith(".war")) {
				populateSrcMainWebapp();
			}
			List<String> classpath = new ArrayList<>();
			classpath.add(targetClasses.getAbsolutePath());
			for (File dependency : dependencies.listFiles()) {
				classpath.add(dependency.getAbsolutePath());
			}
			classpath.add(resourcesProject.getAbsolutePath());
			return Arrays.asList("-cp", StringUtils.collectionToDelimitedString(classpath, File.pathSeparator),
					"com.example.ResourceHandlingApplication");
		} catch (IOException ex) {
			throw new RuntimeException(ex);
		}
	}

	private File populateTargetClasses(File archive) throws IOException {
		File targetClasses = new File(this.exploded, "target/classes");
		targetClasses.mkdirs();
		File source = new File(this.exploded, getClassesPath(archive));
		FileSystemUtils.copyRecursively(source, targetClasses);
		FileSystemUtils.deleteRecursively(source);
		return targetClasses;
	}

	private File populateDependencies(File archive) throws IOException {
		File dependencies = new File(this.exploded, "dependencies");
		dependencies.mkdirs();
		List<String> libPaths = getLibPaths(archive);
		for (String libPath : libPaths) {
			File libDirectory = new File(this.exploded, libPath);
			for (File jar : libDirectory.listFiles()) {
				FileCopyUtils.copy(jar, new File(dependencies, jar.getName()));
			}
			FileSystemUtils.deleteRecursively(libDirectory);
		}
		return dependencies;
	}

	private File explodedResourcesProject(File dependencies) throws IOException {
		File resourcesProject = new File(this.exploded, "resources-project/target/classes");
		File resourcesJar = new File(dependencies, "resources-1.0.jar");
		explodeArchive(resourcesJar, resourcesProject);
		resourcesJar.delete();
		return resourcesProject;
	}

	private void populateSrcMainWebapp() throws IOException {
		File srcMainWebapp = new File(this.exploded, "src/main/webapp");
		srcMainWebapp.mkdirs();
		File source = new File(this.exploded, "webapp-resource.txt");
		FileCopyUtils.copy(source, new File(srcMainWebapp, "webapp-resource.txt"));
		source.delete();
	}

	private void deleteLauncherClasses() {
		FileSystemUtils.deleteRecursively(new File(this.exploded, "org"));
	}

	private String getClassesPath(File archive) {
		return (archive.getName().endsWith(".jar") ? "BOOT-INF/classes" : "WEB-INF/classes");
	}

	private List<String> getLibPaths(File archive) {
		return (archive.getName().endsWith(".jar") ? Collections.singletonList("BOOT-INF/lib")
				: Arrays.asList("WEB-INF/lib", "WEB-INF/lib-provided"));
	}

	private void explodeArchive(File archive, File destination) throws IOException {
		FileSystemUtils.deleteRecursively(destination);
		JarFile jarFile = new JarFile(archive);
		Enumeration<JarEntry> entries = jarFile.entries();
		while (entries.hasMoreElements()) {
			JarEntry jarEntry = entries.nextElement();
			File extracted = new File(destination, jarEntry.getName());
			if (jarEntry.isDirectory()) {
				extracted.mkdirs();
			} else {
				FileOutputStream extractedOutputStream = new FileOutputStream(extracted);
				StreamUtils.copy(jarFile.getInputStream(jarEntry), extractedOutputStream);
				extractedOutputStream.close();
			}
		}
		jarFile.close();
	}

}
