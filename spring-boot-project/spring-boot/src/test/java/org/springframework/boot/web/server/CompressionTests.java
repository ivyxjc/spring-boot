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

package org.springframework.boot.web.server;

import org.apache.coyote.http11.Http11NioProtocol;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.testsupport.runner.classpath.ModifiedClassPathRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link Compression}.
 *
 * @author Andy Wilkinson
 */
@RunWith(ModifiedClassPathRunner.class)
public class CompressionTests {

	@Test
	public void defaultCompressableMimeTypesMatchesTomcatsDefault() {
		assertThat(new Compression().getMimeTypes()).containsExactlyInAnyOrder(getTomcatDefaultCompressableMimeTypes());
	}

	private String[] getTomcatDefaultCompressableMimeTypes() {
		Http11NioProtocol protocol = new Http11NioProtocol();
		return protocol.getCompressibleMimeTypes();
	}

}
