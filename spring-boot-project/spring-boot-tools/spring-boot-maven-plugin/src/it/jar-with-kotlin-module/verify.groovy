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


import org.springframework.boot.maven.Verify

File f = new File(basedir, "target/jar-with-kotlin-module-0.0.1.BUILD-SNAPSHOT.jar")
new Verify.JarArchiveVerification(f, Verify.SAMPLE_APP) {
	@Override
	protected void verifyZipEntries(Verify.ArchiveVerifier verifier) throws Exception {
		super.verifyZipEntries(verifier)
		verifier.assertHasEntryNameStartingWith("BOOT-INF/classes/META-INF/jar-with-kotlin-module.kotlin_module")
		verifier.assertHasUnpackEntry("BOOT-INF/lib/kotlin-compiler-")
		verifier.assertHasNonUnpackEntry("BOOT-INF/lib/kotlin-stdlib-")
		verifier.assertHasNonUnpackEntry("BOOT-INF/lib/kotlin-reflect-")
	}
}.verify()
