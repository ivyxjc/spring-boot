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

package org.springframework.boot.autoconfigure.ldap;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.context.properties.PropertyMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.ldap.core.ContextSource;
import org.springframework.ldap.core.LdapOperations;
import org.springframework.ldap.core.LdapTemplate;
import org.springframework.ldap.core.support.LdapContextSource;

import java.util.Collections;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for LDAP.
 *
 * @author Eddú Meléndez
 * @author Vedran Pavic
 * @since 1.5.0
 */
@Configuration
@ConditionalOnClass(ContextSource.class)
@EnableConfigurationProperties(LdapProperties.class)
public class LdapAutoConfiguration {

	private final LdapProperties properties;

	private final Environment environment;

	public LdapAutoConfiguration(LdapProperties properties, Environment environment) {
		this.properties = properties;
		this.environment = environment;
	}

	@Bean
	@ConditionalOnMissingBean
	public LdapContextSource ldapContextSource() {
		LdapContextSource source = new LdapContextSource();
		PropertyMapper propertyMapper = PropertyMapper.get().alwaysApplyingWhenNonNull();
		propertyMapper.from(this.properties.getUsername()).to(source::setUserDn);
		propertyMapper.from(this.properties.getPassword()).to(source::setPassword);
		propertyMapper.from(this.properties.getAnonymousReadOnly()).to(source::setAnonymousReadOnly);
		propertyMapper.from(this.properties.getBase()).to(source::setBase);
		propertyMapper.from(this.properties.determineUrls(this.environment)).to(source::setUrls);
		propertyMapper.from(this.properties.getBaseEnvironment()).to(
				(baseEnvironment) -> source.setBaseEnvironmentProperties(Collections.unmodifiableMap(baseEnvironment)));
		return source;
	}

	@Bean
	@ConditionalOnMissingBean(LdapOperations.class)
	public LdapTemplate ldapTemplate(ContextSource contextSource) {
		return new LdapTemplate(contextSource);
	}

}
