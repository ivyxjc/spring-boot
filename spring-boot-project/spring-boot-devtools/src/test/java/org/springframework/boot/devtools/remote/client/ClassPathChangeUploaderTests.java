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

package org.springframework.boot.devtools.remote.client;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.springframework.boot.devtools.classpath.ClassPathChangedEvent;
import org.springframework.boot.devtools.filewatch.ChangedFile;
import org.springframework.boot.devtools.filewatch.ChangedFile.Type;
import org.springframework.boot.devtools.filewatch.ChangedFiles;
import org.springframework.boot.devtools.restart.classloader.ClassLoaderFile;
import org.springframework.boot.devtools.restart.classloader.ClassLoaderFile.Kind;
import org.springframework.boot.devtools.restart.classloader.ClassLoaderFiles;
import org.springframework.boot.devtools.restart.classloader.ClassLoaderFiles.SourceFolder;
import org.springframework.boot.devtools.test.MockClientHttpRequestFactory;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.client.MockClientHttpRequest;
import org.springframework.util.FileCopyUtils;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.SocketException;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Tests for {@link ClassPathChangeUploader}.
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 */
public class ClassPathChangeUploaderTests {

	@Rule
	public TemporaryFolder temp = new TemporaryFolder();

	private MockClientHttpRequestFactory requestFactory;

	private ClassPathChangeUploader uploader;

	@Before
	public void setup() {
		this.requestFactory = new MockClientHttpRequestFactory();
		this.uploader = new ClassPathChangeUploader("http://localhost/upload", this.requestFactory);
	}

	@Test
	public void urlMustNotBeNull() {
		assertThatIllegalArgumentException().isThrownBy(() -> new ClassPathChangeUploader(null, this.requestFactory))
				.withMessageContaining("URL must not be empty");
	}

	@Test
	public void urlMustNotBeEmpty() {
		assertThatIllegalArgumentException().isThrownBy(() -> new ClassPathChangeUploader("", this.requestFactory))
				.withMessageContaining("URL must not be empty");
	}

	@Test
	public void requestFactoryMustNotBeNull() {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> new ClassPathChangeUploader("http://localhost:8080", null))
				.withMessageContaining("RequestFactory must not be null");
	}

	@Test
	public void urlMustNotBeMalformed() {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> new ClassPathChangeUploader("htttttp:///ttest", this.requestFactory))
				.withMessageContaining("Malformed URL 'htttttp:///ttest'");
	}

	@Test
	public void sendsClassLoaderFiles() throws Exception {
		File sourceFolder = this.temp.newFolder();
		ClassPathChangedEvent event = createClassPathChangedEvent(sourceFolder);
		this.requestFactory.willRespond(HttpStatus.OK);
		this.uploader.onApplicationEvent(event);
		assertThat(this.requestFactory.getExecutedRequests()).hasSize(1);
		MockClientHttpRequest request = this.requestFactory.getExecutedRequests().get(0);
		verifyUploadRequest(sourceFolder, request);
	}

	@Test
	public void retriesOnSocketException() throws Exception {
		File sourceFolder = this.temp.newFolder();
		ClassPathChangedEvent event = createClassPathChangedEvent(sourceFolder);
		this.requestFactory.willRespond(new SocketException());
		this.requestFactory.willRespond(HttpStatus.OK);
		this.uploader.onApplicationEvent(event);
		assertThat(this.requestFactory.getExecutedRequests()).hasSize(2);
		verifyUploadRequest(sourceFolder, this.requestFactory.getExecutedRequests().get(1));
	}

	private void verifyUploadRequest(File sourceFolder, MockClientHttpRequest request)
			throws IOException, ClassNotFoundException {
		ClassLoaderFiles classLoaderFiles = deserialize(request.getBodyAsBytes());
		Collection<SourceFolder> sourceFolders = classLoaderFiles.getSourceFolders();
		assertThat(sourceFolders.size()).isEqualTo(1);
		SourceFolder classSourceFolder = sourceFolders.iterator().next();
		assertThat(classSourceFolder.getName()).isEqualTo(sourceFolder.getAbsolutePath());
		Iterator<ClassLoaderFile> classFiles = classSourceFolder.getFiles().iterator();
		assertClassFile(classFiles.next(), "File1", ClassLoaderFile.Kind.ADDED);
		assertClassFile(classFiles.next(), "File2", ClassLoaderFile.Kind.MODIFIED);
		assertClassFile(classFiles.next(), null, ClassLoaderFile.Kind.DELETED);
		assertThat(classFiles.hasNext()).isFalse();
	}

	private void assertClassFile(ClassLoaderFile file, String content, Kind kind) {
		assertThat(file.getContents()).isEqualTo((content != null) ? content.getBytes() : null);
		assertThat(file.getKind()).isEqualTo(kind);
	}

	private ClassPathChangedEvent createClassPathChangedEvent(File sourceFolder) throws IOException {
		Set<ChangedFile> files = new LinkedHashSet<>();
		File file1 = createFile(sourceFolder, "File1");
		File file2 = createFile(sourceFolder, "File2");
		File file3 = createFile(sourceFolder, "File3");
		files.add(new ChangedFile(sourceFolder, file1, Type.ADD));
		files.add(new ChangedFile(sourceFolder, file2, Type.MODIFY));
		files.add(new ChangedFile(sourceFolder, file3, Type.DELETE));
		Set<ChangedFiles> changeSet = new LinkedHashSet<>();
		changeSet.add(new ChangedFiles(sourceFolder, files));
		ClassPathChangedEvent event = new ClassPathChangedEvent(this, changeSet, false);
		return event;
	}

	private File createFile(File sourceFolder, String name) throws IOException {
		File file = new File(sourceFolder, name);
		FileCopyUtils.copy(name.getBytes(), file);
		return file;
	}

	private ClassLoaderFiles deserialize(byte[] bytes) throws IOException, ClassNotFoundException {
		ObjectInputStream objectInputStream = new ObjectInputStream(new ByteArrayInputStream(bytes));
		return (ClassLoaderFiles) objectInputStream.readObject();
	}

}
