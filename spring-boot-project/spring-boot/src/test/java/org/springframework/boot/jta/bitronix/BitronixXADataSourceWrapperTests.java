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

package org.springframework.boot.jta.bitronix;

import org.junit.Test;

import javax.sql.DataSource;
import javax.sql.XADataSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link BitronixXADataSourceWrapper}.
 *
 * @author Phillip Webb
 */
public class BitronixXADataSourceWrapperTests {

	@Test
	public void wrap() throws Exception {
		XADataSource dataSource = mock(XADataSource.class);
		BitronixXADataSourceWrapper wrapper = new BitronixXADataSourceWrapper();
		DataSource wrapped = wrapper.wrapDataSource(dataSource);
		assertThat(wrapped).isInstanceOf(PoolingDataSourceBean.class);
		assertThat(((PoolingDataSourceBean) wrapped).getDataSource()).isSameAs(dataSource);
	}

}
