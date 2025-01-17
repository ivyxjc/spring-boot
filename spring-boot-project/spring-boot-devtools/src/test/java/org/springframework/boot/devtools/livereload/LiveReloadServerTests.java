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

package org.springframework.boot.devtools.livereload;

import org.apache.tomcat.websocket.WsWebSocketContainer;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.socket.*;
import org.springframework.web.socket.client.WebSocketClient;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link LiveReloadServer}.
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 */
public class LiveReloadServerTests {

	private static final String HANDSHAKE = "{command: 'hello', "
			+ "protocols: ['http://livereload.com/protocols/official-7']}";

	private int port;

	private MonitoredLiveReloadServer server;

	/**
	 * Useful main method for manual testing against a real browser.
	 *
	 * @param args main args
	 * @throws IOException in case of I/O errors
	 */
	public static void main(String[] args) throws IOException {
		LiveReloadServer server = new LiveReloadServer();
		server.start();
		while (true) {
			try {
				Thread.sleep(1000);
			} catch (InterruptedException ex) {
				Thread.currentThread().interrupt();
			}
			server.triggerReload();
		}
	}

	@Before
	public void setUp() throws Exception {
		this.server = new MonitoredLiveReloadServer(0);
		this.port = this.server.start();
	}

	@After
	public void tearDown() throws Exception {
		this.server.stop();
	}

	@Test
	@Ignore
	public void servesLivereloadJs() throws Exception {
		RestTemplate template = new RestTemplate();
		URI uri = new URI("http://localhost:" + this.port + "/livereload.js");
		String script = template.getForObject(uri, String.class);
		assertThat(script).contains("livereload.com/protocols/official-7");
	}

	@Test
	public void triggerReload() throws Exception {
		LiveReloadWebSocketHandler handler = connect();
		this.server.triggerReload();
		Thread.sleep(200);
		this.server.stop();
		assertThat(handler.getMessages().get(0)).contains("http://livereload.com/protocols/official-7");
		assertThat(handler.getMessages().get(1)).contains("command\":\"reload\"");
	}

	@Test
	public void pingPong() throws Exception {
		LiveReloadWebSocketHandler handler = connect();
		handler.sendMessage(new PingMessage());
		Thread.sleep(200);
		assertThat(handler.getPongCount()).isEqualTo(1);
		this.server.stop();
	}

	@Test
	public void clientClose() throws Exception {
		LiveReloadWebSocketHandler handler = connect();
		handler.close();
		awaitClosedException();
		assertThat(this.server.getClosedExceptions().size()).isGreaterThan(0);
	}

	private void awaitClosedException() throws InterruptedException {
		long startTime = System.currentTimeMillis();
		while (this.server.getClosedExceptions().isEmpty() && System.currentTimeMillis() - startTime < 10000) {
			Thread.sleep(100);
		}
	}

	@Test
	public void serverClose() throws Exception {
		LiveReloadWebSocketHandler handler = connect();
		this.server.stop();
		Thread.sleep(200);
		assertThat(handler.getCloseStatus().getCode()).isEqualTo(1006);
	}

	private LiveReloadWebSocketHandler connect() throws Exception {
		WebSocketClient client = new StandardWebSocketClient(new WsWebSocketContainer());
		LiveReloadWebSocketHandler handler = new LiveReloadWebSocketHandler();
		client.doHandshake(handler, "ws://localhost:" + this.port + "/livereload");
		handler.awaitHello();
		return handler;
	}

	/**
	 * {@link LiveReloadServer} with additional monitoring.
	 */
	private static class MonitoredLiveReloadServer extends LiveReloadServer {

		private final List<ConnectionClosedException> closedExceptions = new ArrayList<>();

		private final Object monitor = new Object();

		MonitoredLiveReloadServer(int port) {
			super(port);
		}

		@Override
		protected Connection createConnection(java.net.Socket socket, InputStream inputStream,
											  OutputStream outputStream) throws IOException {
			return new MonitoredConnection(socket, inputStream, outputStream);
		}

		public List<ConnectionClosedException> getClosedExceptions() {
			synchronized (this.monitor) {
				return new ArrayList<>(this.closedExceptions);
			}
		}

		private class MonitoredConnection extends Connection {

			MonitoredConnection(java.net.Socket socket, InputStream inputStream, OutputStream outputStream)
					throws IOException {
				super(socket, inputStream, outputStream);
			}

			@Override
			public void run() throws Exception {
				try {
					super.run();
				} catch (ConnectionClosedException ex) {
					synchronized (MonitoredLiveReloadServer.this.monitor) {
						MonitoredLiveReloadServer.this.closedExceptions.add(ex);
					}
					throw ex;
				}
			}

		}

	}

	private static class LiveReloadWebSocketHandler extends TextWebSocketHandler {

		private final CountDownLatch helloLatch = new CountDownLatch(2);
		private final List<String> messages = new ArrayList<>();
		private WebSocketSession session;
		private int pongCount;

		private CloseStatus closeStatus;

		@Override
		public void afterConnectionEstablished(WebSocketSession session) throws Exception {
			this.session = session;
			session.sendMessage(new TextMessage(HANDSHAKE));
			this.helloLatch.countDown();
		}

		public void awaitHello() throws InterruptedException {
			this.helloLatch.await(1, TimeUnit.MINUTES);
			Thread.sleep(200);
		}

		@Override
		protected void handleTextMessage(WebSocketSession session, TextMessage message) {
			if (message.getPayload().contains("hello")) {
				this.helloLatch.countDown();
			}
			this.messages.add(message.getPayload());
		}

		@Override
		protected void handlePongMessage(WebSocketSession session, PongMessage message) {
			this.pongCount++;
		}

		@Override
		public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
			this.closeStatus = status;
		}

		public void sendMessage(WebSocketMessage<?> message) throws IOException {
			this.session.sendMessage(message);
		}

		public void close() throws IOException {
			this.session.close();
		}

		public List<String> getMessages() {
			return this.messages;
		}

		public int getPongCount() {
			return this.pongCount;
		}

		public CloseStatus getCloseStatus() {
			return this.closeStatus;
		}

	}

}
