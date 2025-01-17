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

package org.springframework.boot.autoconfigure.http.codec;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.http.HttpProperties;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.codec.CodecCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.codec.CodecConfigurer;
import org.springframework.http.codec.json.Jackson2JsonDecoder;
import org.springframework.http.codec.json.Jackson2JsonEncoder;
import org.springframework.util.MimeType;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for
 * {@link org.springframework.core.codec.Encoder Encoders} and
 * {@link org.springframework.core.codec.Decoder Decoders}.
 *
 * @author Brian Clozel
 * @since 2.0.0
 */
@Configuration
@ConditionalOnClass(CodecConfigurer.class)
@AutoConfigureAfter(JacksonAutoConfiguration.class)
public class CodecsAutoConfiguration {

	private static final MimeType[] EMPTY_MIME_TYPES = {};

	@Configuration
	@ConditionalOnClass(ObjectMapper.class)
	static class JacksonCodecConfiguration {

		@Bean
		@Order(0)
		@ConditionalOnBean(ObjectMapper.class)
		public CodecCustomizer jacksonCodecCustomizer(ObjectMapper objectMapper) {
			return (configurer) -> {
				CodecConfigurer.DefaultCodecs defaults = configurer.defaultCodecs();
				defaults.jackson2JsonDecoder(new Jackson2JsonDecoder(objectMapper, EMPTY_MIME_TYPES));
				defaults.jackson2JsonEncoder(new Jackson2JsonEncoder(objectMapper, EMPTY_MIME_TYPES));
			};
		}

	}

	@Configuration
	@EnableConfigurationProperties(HttpProperties.class)
	static class LoggingCodecConfiguration {

		@Bean
		@Order(0)
		public CodecCustomizer loggingCodecCustomizer(HttpProperties properties) {
			return (configurer) -> configurer.defaultCodecs()
					.enableLoggingRequestDetails(properties.isLogRequestDetails());
		}

	}

}
