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

package org.springframework.boot.context.logging;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.event.ApplicationStartingEvent;
import org.springframework.boot.logging.LogFile;
import org.springframework.boot.logging.LoggingSystem;
import org.springframework.boot.testsupport.rule.OutputCapture;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Component;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link LoggingApplicationListener}.
 *
 * @author Stephane Nicoll
 */
public class LoggingApplicationListenerIntegrationTests {

	@Rule
	public final TemporaryFolder temp = new TemporaryFolder();
	@Rule
	public OutputCapture outputCapture = new OutputCapture();

	@Test
	public void loggingSystemRegisteredInTheContext() {
		try (ConfigurableApplicationContext context = new SpringApplicationBuilder(SampleService.class)
				.web(WebApplicationType.NONE).run()) {
			SampleService service = context.getBean(SampleService.class);
			assertThat(service.loggingSystem).isNotNull();
		}
	}

	@Test
	public void logFileRegisteredInTheContextWhenApplicable() throws Exception {
		String logFile = this.temp.newFile().getAbsolutePath();
		try (ConfigurableApplicationContext context = new SpringApplicationBuilder(SampleService.class)
				.web(WebApplicationType.NONE).properties("logging.file=" + logFile).run()) {
			SampleService service = context.getBean(SampleService.class);
			assertThat(service.logFile).isNotNull();
			assertThat(service.logFile.toString()).isEqualTo(logFile);
		}
	}

	@Test
	public void loggingPerformedDuringChildApplicationStartIsNotLost() {
		new SpringApplicationBuilder(Config.class).web(WebApplicationType.NONE).child(Config.class)
				.web(WebApplicationType.NONE).listeners(new ApplicationListener<ApplicationStartingEvent>() {

			private final Logger logger = LoggerFactory.getLogger(getClass());

			@Override
			public void onApplicationEvent(ApplicationStartingEvent event) {
				this.logger.info("Child application starting");
			}

		}).run();
		assertThat(this.outputCapture.toString()).contains("Child application starting");
	}

	@Component
	static class SampleService {

		private final LoggingSystem loggingSystem;

		private final LogFile logFile;

		SampleService(LoggingSystem loggingSystem, ObjectProvider<LogFile> logFile) {
			this.loggingSystem = loggingSystem;
			this.logFile = logFile.getIfAvailable();
		}

	}

	static class Config {

	}

}
