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

package org.springframework.boot.devtools.filewatch;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.springframework.util.FileCopyUtils;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Tests for {@link FileSnapshot}.
 *
 * @author Phillip Webb
 */
public class FileSnapshotTests {

	private static final long TWO_MINS = TimeUnit.MINUTES.toMillis(2);

	private static final long MODIFIED = new Date().getTime() - TimeUnit.DAYS.toMillis(10);

	@Rule
	public TemporaryFolder temporaryFolder = new TemporaryFolder();

	@Test
	public void fileMustNotBeNull() {
		assertThatIllegalArgumentException().isThrownBy(() -> new FileSnapshot(null))
				.withMessageContaining("File must not be null");
	}

	@Test
	public void fileMustNotBeAFolder() throws Exception {
		assertThatIllegalArgumentException().isThrownBy(() -> new FileSnapshot(this.temporaryFolder.newFolder()))
				.withMessageContaining("File must not be a folder");
	}

	@Test
	public void equalsIfTheSame() throws Exception {
		File file = createNewFile("abc", MODIFIED);
		File fileCopy = new File(file, "x").getParentFile();
		FileSnapshot snapshot1 = new FileSnapshot(file);
		FileSnapshot snapshot2 = new FileSnapshot(fileCopy);
		assertThat(snapshot1).isEqualTo(snapshot2);
		assertThat(snapshot1.hashCode()).isEqualTo(snapshot2.hashCode());
	}

	@Test
	public void notEqualsIfDeleted() throws Exception {
		File file = createNewFile("abc", MODIFIED);
		FileSnapshot snapshot1 = new FileSnapshot(file);
		file.delete();
		assertThat(snapshot1).isNotEqualTo(new FileSnapshot(file));
	}

	@Test
	public void notEqualsIfLengthChanges() throws Exception {
		File file = createNewFile("abc", MODIFIED);
		FileSnapshot snapshot1 = new FileSnapshot(file);
		setupFile(file, "abcd", MODIFIED);
		assertThat(snapshot1).isNotEqualTo(new FileSnapshot(file));
	}

	@Test
	public void notEqualsIfLastModifiedChanges() throws Exception {
		File file = createNewFile("abc", MODIFIED);
		FileSnapshot snapshot1 = new FileSnapshot(file);
		setupFile(file, "abc", MODIFIED + TWO_MINS);
		assertThat(snapshot1).isNotEqualTo(new FileSnapshot(file));
	}

	private File createNewFile(String content, long lastModified) throws IOException {
		File file = this.temporaryFolder.newFile();
		setupFile(file, content, lastModified);
		return file;
	}

	private void setupFile(File file, String content, long lastModified) throws IOException {
		FileCopyUtils.copy(content.getBytes(), file);
		file.setLastModified(lastModified);
	}

}
