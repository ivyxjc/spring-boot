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

package org.springframework.boot.actuate.health;

import org.junit.Test;
import org.mockito.stubbing.Answer;
import org.springframework.boot.actuate.endpoint.SecurityContext;
import org.springframework.boot.actuate.endpoint.web.WebEndpointResponse;
import org.springframework.http.HttpStatus;

import java.security.Principal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

/**
 * Tests for {@link HealthWebEndpointResponseMapper}.
 *
 * @author Stephane Nicoll
 */
public class HealthWebEndpointResponseMapperTests {

	private final HealthStatusHttpMapper statusHttpMapper = new HealthStatusHttpMapper();

	private Set<String> authorizedRoles = Collections.singleton("ACTUATOR");

	@Test
	public void mapDetailsWithDisableDetailsDoesNotInvokeSupplier() {
		HealthWebEndpointResponseMapper mapper = createMapper(ShowDetails.NEVER);
		Supplier<Health> supplier = mockSupplier();
		SecurityContext securityContext = mock(SecurityContext.class);
		WebEndpointResponse<Health> response = mapper.mapDetails(supplier, securityContext);
		assertThat(response.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
		verifyZeroInteractions(supplier);
		verifyZeroInteractions(securityContext);
	}

	@Test
	public void mapDetailsWithUnauthorizedUserDoesNotInvokeSupplier() {
		HealthWebEndpointResponseMapper mapper = createMapper(ShowDetails.WHEN_AUTHORIZED);
		Supplier<Health> supplier = mockSupplier();
		SecurityContext securityContext = mockSecurityContext("USER");
		WebEndpointResponse<Health> response = mapper.mapDetails(supplier, securityContext);
		assertThat(response.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
		assertThat(response.getBody()).isNull();
		verifyZeroInteractions(supplier);
		verify(securityContext).isUserInRole("ACTUATOR");
	}

	@Test
	public void mapDetailsWithAuthorizedUserInvokeSupplier() {
		HealthWebEndpointResponseMapper mapper = createMapper(ShowDetails.WHEN_AUTHORIZED);
		Supplier<Health> supplier = mockSupplier();
		given(supplier.get()).willReturn(Health.down().build());
		SecurityContext securityContext = mockSecurityContext("ACTUATOR");
		WebEndpointResponse<Health> response = mapper.mapDetails(supplier, securityContext);
		assertThat(response.getStatus()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE.value());
		assertThat(response.getBody().getStatus()).isEqualTo(Status.DOWN);
		verify(supplier).get();
		verify(securityContext).isUserInRole("ACTUATOR");
	}

	@Test
	public void mapDetailsWithUnavailableHealth() {
		HealthWebEndpointResponseMapper mapper = createMapper(ShowDetails.ALWAYS);
		Supplier<Health> supplier = mockSupplier();
		SecurityContext securityContext = mock(SecurityContext.class);
		WebEndpointResponse<Health> response = mapper.mapDetails(supplier, securityContext);
		assertThat(response.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
		assertThat(response.getBody()).isNull();
		verify(supplier).get();
		verifyZeroInteractions(securityContext);
	}

	@SuppressWarnings("unchecked")
	private Supplier<Health> mockSupplier() {
		return mock(Supplier.class);
	}

	private SecurityContext mockSecurityContext(String... roles) {
		List<String> associatedRoles = Arrays.asList(roles);
		SecurityContext securityContext = mock(SecurityContext.class);
		given(securityContext.getPrincipal()).willReturn(mock(Principal.class));
		given(securityContext.isUserInRole(anyString())).will((Answer<Boolean>) (invocation) -> {
			String expectedRole = invocation.getArgument(0);
			return associatedRoles.contains(expectedRole);
		});
		return securityContext;
	}

	private HealthWebEndpointResponseMapper createMapper(ShowDetails showDetails) {
		return new HealthWebEndpointResponseMapper(this.statusHttpMapper, showDetails, this.authorizedRoles);
	}

}
