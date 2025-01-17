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

package org.springframework.boot.convert;

import org.junit.AssumptionViolatedException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.springframework.core.convert.ConversionFailedException;
import org.springframework.core.convert.ConversionService;

import java.net.InetAddress;
import java.net.UnknownHostException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Tests for {@link InetAddressFormatter}.
 *
 * @author Phillip Webb
 */
@RunWith(Parameterized.class)
public class InetAddressFormatterTests {

	private final ConversionService conversionService;

	public InetAddressFormatterTests(String name, ConversionService conversionService) {
		this.conversionService = conversionService;
	}

	@Parameters(name = "{0}")
	public static Iterable<Object[]> conversionServices() {
		return new ConversionServiceParameters(new InetAddressFormatter());
	}

	@Test
	public void convertFromInetAddressToStringShouldConvert() throws UnknownHostException {
		assumeResolves("example.com", true);
		InetAddress address = InetAddress.getByName("example.com");
		String converted = this.conversionService.convert(address, String.class);
		assertThat(converted).isEqualTo(address.getHostAddress());
	}

	@Test
	public void convertFromStringToInetAddressShouldConvert() {
		assumeResolves("example.com", true);
		InetAddress converted = this.conversionService.convert("example.com", InetAddress.class);
		assertThat(converted.toString()).startsWith("example.com");
	}

	@Test
	public void convertFromStringToInetAddressWhenHostDoesNotExistShouldThrowException() {
		String missingDomain = "ireallydontexist.example.com";
		assumeResolves(missingDomain, false);
		assertThatExceptionOfType(ConversionFailedException.class)
				.isThrownBy(() -> this.conversionService.convert(missingDomain, InetAddress.class));
	}

	private void assumeResolves(String host, boolean expectedToResolve) {
		boolean resolved = isResolvable(host);
		if (resolved != expectedToResolve) {
			throw new AssumptionViolatedException("Host " + host + " resolved " + resolved);
		}
	}

	private boolean isResolvable(String host) {
		try {
			InetAddress.getByName(host);
			return true;
		} catch (UnknownHostException ex) {
			return false;
		}
	}

}
