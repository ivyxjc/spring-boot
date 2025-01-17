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

package org.springframework.boot.devtools.restart;

import org.junit.Test;

import java.util.concurrent.CountDownLatch;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

/**
 * Tests for {@link SilentExitExceptionHandler}.
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 */
public class SilentExitExceptionHandlerTests {

	@Test
	public void setupAndExit() throws Exception {
		TestThread testThread = new TestThread() {
			@Override
			public void run() {
				SilentExitExceptionHandler.exitCurrentThread();
				fail("Didn't exit");
			}
		};
		SilentExitExceptionHandler.setup(testThread);
		testThread.startAndJoin();
		assertThat(testThread.getThrown()).isNull();
	}

	@Test
	public void doesntInterfereWithOtherExceptions() throws Exception {
		TestThread testThread = new TestThread() {
			@Override
			public void run() {
				throw new IllegalStateException("Expected");
			}
		};
		SilentExitExceptionHandler.setup(testThread);
		testThread.startAndJoin();
		assertThat(testThread.getThrown().getMessage()).isEqualTo("Expected");
	}

	@Test
	public void preventsNonZeroExitCodeWhenAllOtherThreadsAreDaemonThreads() {
		try {
			SilentExitExceptionHandler.exitCurrentThread();
		} catch (Exception ex) {
			TestSilentExitExceptionHandler silentExitExceptionHandler = new TestSilentExitExceptionHandler();
			silentExitExceptionHandler.uncaughtException(Thread.currentThread(), ex);
			try {
				assertThat(silentExitExceptionHandler.nonZeroExitCodePrevented).isTrue();
			} finally {
				silentExitExceptionHandler.cleanUp();
			}
		}

	}

	private abstract static class TestThread extends Thread {

		private Throwable thrown;

		TestThread() {
			setUncaughtExceptionHandler((thread, exception) -> TestThread.this.thrown = exception);
		}

		public Throwable getThrown() {
			return this.thrown;
		}

		public void startAndJoin() throws InterruptedException {
			start();
			join();
		}

	}

	private static class TestSilentExitExceptionHandler extends SilentExitExceptionHandler {

		private final Object monitor = new Object();
		private boolean nonZeroExitCodePrevented;

		TestSilentExitExceptionHandler() {
			super(null);
		}

		@Override
		protected void preventNonZeroExitCode() {
			this.nonZeroExitCodePrevented = true;
		}

		@Override
		protected Thread[] getAllThreads() {
			final CountDownLatch threadRunning = new CountDownLatch(1);
			Thread daemonThread = new Thread(() -> {
				synchronized (TestSilentExitExceptionHandler.this.monitor) {
					threadRunning.countDown();
					try {
						TestSilentExitExceptionHandler.this.monitor.wait();
					} catch (InterruptedException ex) {
						Thread.currentThread().interrupt();
					}
				}
			});
			daemonThread.setDaemon(true);
			daemonThread.start();
			try {
				threadRunning.await();
			} catch (InterruptedException ex) {
				Thread.currentThread().interrupt();
			}
			return new Thread[]{Thread.currentThread(), daemonThread};
		}

		private void cleanUp() {
			synchronized (this.monitor) {
				this.monitor.notifyAll();
			}
		}

	}

}
