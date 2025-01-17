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

package org.springframework.boot.web.embedded.tomcat;

import org.apache.catalina.*;
import org.apache.catalina.connector.Connector;
import org.apache.catalina.core.AprLifecycleListener;
import org.apache.catalina.core.StandardContext;
import org.apache.catalina.core.StandardWrapper;
import org.apache.catalina.startup.Tomcat;
import org.apache.catalina.util.CharsetMapper;
import org.apache.catalina.valves.RemoteIpValve;
import org.apache.catalina.webresources.TomcatURLStreamHandlerFactory;
import org.apache.jasper.servlet.JspServlet;
import org.apache.tomcat.JarScanFilter;
import org.apache.tomcat.JarScanType;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.springframework.boot.testsupport.rule.OutputCapture;
import org.springframework.boot.web.server.WebServerException;
import org.springframework.boot.web.servlet.ServletContextInitializer;
import org.springframework.boot.web.servlet.server.AbstractServletWebServerFactory;
import org.springframework.boot.web.servlet.server.AbstractServletWebServerFactoryTests;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.util.FileSystemUtils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.MultipartConfigElement;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRegistration.Dynamic;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Tests for {@link TomcatServletWebServerFactory}.
 *
 * @author Phillip Webb
 * @author Dave Syer
 * @author Stephane Nicoll
 */
public class TomcatServletWebServerFactoryTests extends AbstractServletWebServerFactoryTests {

	@Rule
	public OutputCapture outputCapture = new OutputCapture();

	@Override
	protected TomcatServletWebServerFactory getFactory() {
		return new TomcatServletWebServerFactory(0);
	}

	@After
	public void restoreTccl() {
		ReflectionTestUtils.setField(TomcatURLStreamHandlerFactory.class, "instance", null);
		ReflectionTestUtils.setField(URL.class, "factory", null);
		Thread.currentThread().setContextClassLoader(getClass().getClassLoader());
	}

	// JMX MBean names clash if you get more than one Engine with the same name...
	@Test
	public void tomcatEngineNames() {
		TomcatServletWebServerFactory factory = getFactory();
		this.webServer = factory.getWebServer();
		factory.setPort(0);
		TomcatWebServer tomcatWebServer = (TomcatWebServer) factory.getWebServer();
		// Make sure that the names are different
		String firstName = ((TomcatWebServer) this.webServer).getTomcat().getEngine().getName();
		String secondName = tomcatWebServer.getTomcat().getEngine().getName();
		assertThat(firstName).as("Tomcat engines must have different names").isNotEqualTo(secondName);
		tomcatWebServer.stop();
	}

	@Test
	public void defaultTomcatListeners() {
		TomcatServletWebServerFactory factory = getFactory();
		if (AprLifecycleListener.isAprAvailable()) {
			assertThat(factory.getContextLifecycleListeners()).hasSize(1).first()
					.isInstanceOf(AprLifecycleListener.class);
		} else {
			assertThat(factory.getContextLifecycleListeners()).isEmpty();
		}
	}

	@Test
	public void tomcatListeners() {
		TomcatServletWebServerFactory factory = getFactory();
		LifecycleListener[] listeners = new LifecycleListener[4];
		Arrays.setAll(listeners, (i) -> mock(LifecycleListener.class));
		factory.setContextLifecycleListeners(Arrays.asList(listeners[0], listeners[1]));
		factory.addContextLifecycleListeners(listeners[2], listeners[3]);
		this.webServer = factory.getWebServer();
		InOrder ordered = inOrder((Object[]) listeners);
		for (LifecycleListener listener : listeners) {
			ordered.verify(listener).lifecycleEvent(any(LifecycleEvent.class));
		}
	}

	@Test
	public void tomcatCustomizers() {
		TomcatServletWebServerFactory factory = getFactory();
		TomcatContextCustomizer[] listeners = new TomcatContextCustomizer[4];
		Arrays.setAll(listeners, (i) -> mock(TomcatContextCustomizer.class));
		factory.setTomcatContextCustomizers(Arrays.asList(listeners[0], listeners[1]));
		factory.addContextCustomizers(listeners[2], listeners[3]);
		this.webServer = factory.getWebServer();
		InOrder ordered = inOrder((Object[]) listeners);
		for (TomcatContextCustomizer listener : listeners) {
			ordered.verify(listener).customize(any(Context.class));
		}
	}

	@Test
	public void contextIsAddedToHostBeforeCustomizersAreCalled() {
		TomcatServletWebServerFactory factory = getFactory();
		TomcatContextCustomizer customizer = mock(TomcatContextCustomizer.class);
		factory.addContextCustomizers(customizer);
		this.webServer = factory.getWebServer();
		ArgumentCaptor<Context> contextCaptor = ArgumentCaptor.forClass(Context.class);
		verify(customizer).customize(contextCaptor.capture());
		assertThat(contextCaptor.getValue().getParent()).isNotNull();
	}

	@Test
	public void tomcatConnectorCustomizers() {
		TomcatServletWebServerFactory factory = getFactory();
		TomcatConnectorCustomizer[] listeners = new TomcatConnectorCustomizer[4];
		Arrays.setAll(listeners, (i) -> mock(TomcatConnectorCustomizer.class));
		factory.setTomcatConnectorCustomizers(Arrays.asList(listeners[0], listeners[1]));
		factory.addConnectorCustomizers(listeners[2], listeners[3]);
		this.webServer = factory.getWebServer();
		InOrder ordered = inOrder((Object[]) listeners);
		for (TomcatConnectorCustomizer listener : listeners) {
			ordered.verify(listener).customize(any(Connector.class));
		}
	}

	@Test
	public void tomcatAdditionalConnectors() {
		TomcatServletWebServerFactory factory = getFactory();
		Connector[] listeners = new Connector[4];
		Arrays.setAll(listeners, (i) -> new Connector());
		factory.addAdditionalTomcatConnectors(listeners);
		this.webServer = factory.getWebServer();
		Map<Service, Connector[]> connectors = ((TomcatWebServer) this.webServer).getServiceConnectors();
		assertThat(connectors.values().iterator().next().length).isEqualTo(listeners.length + 1);
	}

	@Test
	public void addNullAdditionalConnectorThrows() {
		TomcatServletWebServerFactory factory = getFactory();
		assertThatIllegalArgumentException().isThrownBy(() -> factory.addAdditionalTomcatConnectors((Connector[]) null))
				.withMessageContaining("Connectors must not be null");
	}

	@Test
	public void sessionTimeout() {
		TomcatServletWebServerFactory factory = getFactory();
		factory.getSession().setTimeout(Duration.ofSeconds(10));
		assertTimeout(factory, 1);
	}

	@Test
	public void sessionTimeoutInMins() {
		TomcatServletWebServerFactory factory = getFactory();
		factory.getSession().setTimeout(Duration.ofMinutes(1));
		assertTimeout(factory, 1);
	}

	@Test
	public void noSessionTimeout() {
		TomcatServletWebServerFactory factory = getFactory();
		factory.getSession().setTimeout(null);
		assertTimeout(factory, -1);
	}

	@Test
	public void valve() {
		TomcatServletWebServerFactory factory = getFactory();
		Valve valve = mock(Valve.class);
		factory.addContextValves(valve);
		this.webServer = factory.getWebServer();
		verify(valve).setNext(any(Valve.class));
	}

	@Test
	public void setNullTomcatContextCustomizersThrows() {
		TomcatServletWebServerFactory factory = getFactory();
		assertThatIllegalArgumentException().isThrownBy(() -> factory.setTomcatContextCustomizers(null))
				.withMessageContaining("TomcatContextCustomizers must not be null");
	}

	@Test
	public void addNullContextCustomizersThrows() {
		TomcatServletWebServerFactory factory = getFactory();
		assertThatIllegalArgumentException()
				.isThrownBy(() -> factory.addContextCustomizers((TomcatContextCustomizer[]) null))
				.withMessageContaining("TomcatContextCustomizers must not be null");
	}

	@Test
	public void setNullTomcatConnectorCustomizersThrows() {
		TomcatServletWebServerFactory factory = getFactory();
		assertThatIllegalArgumentException().isThrownBy(() -> factory.setTomcatConnectorCustomizers(null))
				.withMessageContaining("TomcatConnectorCustomizers must not be null");
	}

	@Test
	public void addNullConnectorCustomizersThrows() {
		TomcatServletWebServerFactory factory = getFactory();
		assertThatIllegalArgumentException()
				.isThrownBy(() -> factory.addConnectorCustomizers((TomcatConnectorCustomizer[]) null))
				.withMessageContaining("TomcatConnectorCustomizers must not be null");
	}

	@Test
	public void uriEncoding() {
		TomcatServletWebServerFactory factory = getFactory();
		factory.setUriEncoding(StandardCharsets.US_ASCII);
		Tomcat tomcat = getTomcat(factory);
		Connector connector = ((TomcatWebServer) this.webServer).getServiceConnectors().get(tomcat.getService())[0];
		assertThat(connector.getURIEncoding()).isEqualTo("US-ASCII");
	}

	@Test
	public void defaultUriEncoding() {
		TomcatServletWebServerFactory factory = getFactory();
		Tomcat tomcat = getTomcat(factory);
		Connector connector = ((TomcatWebServer) this.webServer).getServiceConnectors().get(tomcat.getService())[0];
		assertThat(connector.getURIEncoding()).isEqualTo("UTF-8");
	}

	@Test
	public void primaryConnectorPortClashThrowsWebServerException() throws IOException {
		doWithBlockedPort((port) -> {
			TomcatServletWebServerFactory factory = getFactory();
			factory.setPort(port);
			assertThatExceptionOfType(WebServerException.class).isThrownBy(() -> {
				this.webServer = factory.getWebServer();
				this.webServer.start();
			});
		});
	}

	@Test
	public void startupFailureDoesNotResultInUnstoppedThreadsBeingReported() throws IOException {
		super.portClashOfPrimaryConnectorResultsInPortInUseException();
		String string = this.outputCapture.toString();
		assertThat(string).doesNotContain("appears to have started a thread named [main]");
	}

	@Test
	public void stopCalledWithoutStart() {
		TomcatServletWebServerFactory factory = getFactory();
		this.webServer = factory.getWebServer(exampleServletRegistration());
		this.webServer.stop();
		Tomcat tomcat = ((TomcatWebServer) this.webServer).getTomcat();
		assertThat(tomcat.getServer().getState()).isSameAs(LifecycleState.DESTROYED);
	}

	@Override
	protected void addConnector(int port, AbstractServletWebServerFactory factory) {
		Connector connector = new Connector("org.apache.coyote.http11.Http11NioProtocol");
		connector.setPort(port);
		((TomcatServletWebServerFactory) factory).addAdditionalTomcatConnectors(connector);
	}

	@Test
	public void useForwardHeaders() throws Exception {
		TomcatServletWebServerFactory factory = getFactory();
		factory.addContextValves(new RemoteIpValve());
		assertForwardHeaderIsUsed(factory);
	}

	@Test
	public void disableDoesNotSaveSessionFiles() throws Exception {
		File baseDir = this.temporaryFolder.newFolder();
		TomcatServletWebServerFactory factory = getFactory();
		// If baseDir is not set SESSIONS.ser is written to a different temp directory
		// each time. By setting it we can really ensure that data isn't saved
		factory.setBaseDirectory(baseDir);
		this.webServer = factory.getWebServer(sessionServletRegistration());
		this.webServer.start();
		String s1 = getResponse(getLocalUrl("/session"));
		String s2 = getResponse(getLocalUrl("/session"));
		this.webServer.stop();
		this.webServer = factory.getWebServer(sessionServletRegistration());
		this.webServer.start();
		String s3 = getResponse(getLocalUrl("/session"));
		String message = "Session error s1=" + s1 + " s2=" + s2 + " s3=" + s3;
		assertThat(s2.split(":")[0]).as(message).isEqualTo(s1.split(":")[1]);
		assertThat(s3.split(":")[0]).as(message).isNotEqualTo(s2.split(":")[1]);
	}

	@Test
	public void jndiLookupsCanBePerformedDuringApplicationContextRefresh() throws NamingException {
		Thread.currentThread().setContextClassLoader(getClass().getClassLoader());
		TomcatServletWebServerFactory factory = new TomcatServletWebServerFactory(0) {

			@Override
			protected TomcatWebServer getTomcatWebServer(Tomcat tomcat) {
				tomcat.enableNaming();
				return super.getTomcatWebServer(tomcat);
			}

		};
		// Server is created in onRefresh
		this.webServer = factory.getWebServer();
		// Lookups should now be possible
		new InitialContext().lookup("java:comp/env");
		// Called in finishRefresh, giving us an opportunity to remove the context binding
		// and avoid a leak
		this.webServer.start();
		// Lookups should no longer be possible
		assertThatExceptionOfType(NamingException.class).isThrownBy(() -> new InitialContext().lookup("java:comp/env"));
	}

	@Test
	public void defaultLocaleCharsetMappingsAreOverridden() {
		TomcatServletWebServerFactory factory = getFactory();
		this.webServer = factory.getWebServer();
		// override defaults, see org.apache.catalina.util.CharsetMapperDefault.properties
		assertThat(getCharset(Locale.ENGLISH)).isEqualTo(StandardCharsets.UTF_8);
		assertThat(getCharset(Locale.FRENCH)).isEqualTo(StandardCharsets.UTF_8);
	}

	@Test
	public void sessionIdGeneratorIsConfiguredWithAttributesFromTheManager() {
		System.setProperty("jvmRoute", "test");
		try {
			TomcatServletWebServerFactory factory = getFactory();
			this.webServer = factory.getWebServer();
			this.webServer.start();
		} finally {
			System.clearProperty("jvmRoute");
		}
		Tomcat tomcat = ((TomcatWebServer) this.webServer).getTomcat();
		Context context = (Context) tomcat.getHost().findChildren()[0];
		SessionIdGenerator sessionIdGenerator = context.getManager().getSessionIdGenerator();
		assertThat(sessionIdGenerator).isInstanceOf(LazySessionIdGenerator.class);
		assertThat(sessionIdGenerator.getJvmRoute()).isEqualTo("test");
	}

	@Test
	public void tldSkipPatternsShouldBeAppliedToContextJarScanner() {
		TomcatServletWebServerFactory factory = getFactory();
		factory.addTldSkipPatterns("foo.jar", "bar.jar");
		this.webServer = factory.getWebServer();
		this.webServer.start();
		Tomcat tomcat = ((TomcatWebServer) this.webServer).getTomcat();
		Context context = (Context) tomcat.getHost().findChildren()[0];
		JarScanFilter jarScanFilter = context.getJarScanner().getJarScanFilter();
		assertThat(jarScanFilter.check(JarScanType.TLD, "foo.jar")).isFalse();
		assertThat(jarScanFilter.check(JarScanType.TLD, "bar.jar")).isFalse();
		assertThat(jarScanFilter.check(JarScanType.TLD, "test.jar")).isTrue();
	}

	@Test
	public void customTomcatHttpOnlyCookie() {
		TomcatServletWebServerFactory factory = getFactory();
		factory.getSession().getCookie().setHttpOnly(false);
		this.webServer = factory.getWebServer();
		this.webServer.start();
		Tomcat tomcat = ((TomcatWebServer) this.webServer).getTomcat();
		Context context = (Context) tomcat.getHost().findChildren()[0];
		assertThat(context.getUseHttpOnly()).isFalse();
	}

	@Test
	public void exceptionThrownOnLoadFailureWhenFailCtxIfServletStartFailsIsTrue() {
		TomcatServletWebServerFactory factory = getFactory();
		factory.addContextCustomizers((context) -> {
			if (context instanceof StandardContext) {
				((StandardContext) context).setFailCtxIfServletStartFails(true);
			}
		});
		this.webServer = factory
				.getWebServer((context) -> context.addServlet("failing", FailingServlet.class).setLoadOnStartup(0));
		assertThatExceptionOfType(WebServerException.class).isThrownBy(this.webServer::start);
	}

	@Test
	public void exceptionThrownOnLoadFailureWhenFailCtxIfServletStartFailsIsFalse() {
		TomcatServletWebServerFactory factory = getFactory();
		factory.addContextCustomizers((context) -> {
			if (context instanceof StandardContext) {
				((StandardContext) context).setFailCtxIfServletStartFails(false);
			}
		});
		this.webServer = factory
				.getWebServer((context) -> context.addServlet("failing", FailingServlet.class).setLoadOnStartup(0));
		this.webServer.start();
	}

	@Test
	public void referenceClearingIsDisabled() {
		TomcatServletWebServerFactory factory = getFactory();
		this.webServer = factory.getWebServer();
		this.webServer.start();
		Tomcat tomcat = ((TomcatWebServer) this.webServer).getTomcat();
		StandardContext context = (StandardContext) tomcat.getHost().findChildren()[0];
		assertThat(context.getClearReferencesObjectStreamClassCaches()).isFalse();
		assertThat(context.getClearReferencesRmiTargets()).isFalse();
		assertThat(context.getClearReferencesThreadLocals()).isFalse();
	}

	@Test
	public void nonExistentUploadDirectoryIsCreatedUponMultipartUpload() throws IOException, URISyntaxException {
		TomcatServletWebServerFactory factory = new TomcatServletWebServerFactory(0);
		AtomicReference<ServletContext> servletContextReference = new AtomicReference<>();
		factory.addInitializers(new ServletContextInitializer() {

			@Override
			public void onStartup(ServletContext servletContext) throws ServletException {
				servletContextReference.set(servletContext);
				Dynamic servlet = servletContext.addServlet("upload", new HttpServlet() {

					@Override
					protected void doPost(HttpServletRequest req, HttpServletResponse resp)
							throws ServletException, IOException {
						req.getParts();
					}

				});
				servlet.addMapping("/upload");
				servlet.setMultipartConfig(new MultipartConfigElement((String) null));
			}

		});
		this.webServer = factory.getWebServer();
		this.webServer.start();
		File temp = (File) servletContextReference.get().getAttribute(ServletContext.TEMPDIR);
		FileSystemUtils.deleteRecursively(temp);
		RestTemplate restTemplate = new RestTemplate();
		HttpHeaders headers = new HttpHeaders();
		MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
		body.add("file", new ByteArrayResource(new byte[1024 * 1024]));
		headers.setContentType(MediaType.MULTIPART_FORM_DATA);
		HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);
		ResponseEntity<String> response = restTemplate.postForEntity(getLocalUrl("/upload"), requestEntity,
				String.class);
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
	}

	@Test
	public void exceptionThrownOnContextListenerDestroysServer() {
		TomcatServletWebServerFactory factory = new TomcatServletWebServerFactory(0) {

			@Override
			protected TomcatWebServer getTomcatWebServer(Tomcat tomcat) {
				try {
					return super.getTomcatWebServer(tomcat);
				} finally {
					assertThat(tomcat.getServer().getState()).isEqualTo(LifecycleState.DESTROYED);
				}
			}

		};
		assertThatExceptionOfType(WebServerException.class).isThrownBy(
				() -> factory.getWebServer((context) -> context.addListener(new FailingServletContextListener())));
	}

	@Test
	public void registerJspServletWithDefaultLoadOnStartup() {
		TomcatServletWebServerFactory factory = new TomcatServletWebServerFactory(0);
		factory.addInitializers(new ServletContextInitializer() {

			@Override
			public void onStartup(ServletContext servletContext) throws ServletException {
				servletContext.addServlet("manually-registered-jsp-servlet", JspServlet.class);
			}

		});
		this.webServer = factory.getWebServer();
		this.webServer.start();
	}

	@Override
	protected JspServlet getJspServlet() throws ServletException {
		Tomcat tomcat = ((TomcatWebServer) this.webServer).getTomcat();
		Container container = tomcat.getHost().findChildren()[0];
		StandardWrapper standardWrapper = (StandardWrapper) container.findChild("jsp");
		if (standardWrapper == null) {
			return null;
		}
		standardWrapper.load();
		return (JspServlet) standardWrapper.getServlet();
	}

	@Override
	protected Map<String, String> getActualMimeMappings() {
		Context context = (Context) ((TomcatWebServer) this.webServer).getTomcat().getHost().findChildren()[0];
		Map<String, String> mimeMappings = new HashMap<>();
		for (String extension : context.findMimeMappings()) {
			mimeMappings.put(extension, context.findMimeMapping(extension));
		}
		return mimeMappings;
	}

	@Override
	protected Charset getCharset(Locale locale) {
		Context context = (Context) ((TomcatWebServer) this.webServer).getTomcat().getHost().findChildren()[0];
		CharsetMapper mapper = ((TomcatEmbeddedContext) context).getCharsetMapper();
		String charsetName = mapper.getCharset(locale);
		return (charsetName != null) ? Charset.forName(charsetName) : null;
	}

	private void assertTimeout(TomcatServletWebServerFactory factory, int expected) {
		Tomcat tomcat = getTomcat(factory);
		Context context = (Context) tomcat.getHost().findChildren()[0];
		assertThat(context.getSessionTimeout()).isEqualTo(expected);
	}

	private Tomcat getTomcat(TomcatServletWebServerFactory factory) {
		this.webServer = factory.getWebServer();
		return ((TomcatWebServer) this.webServer).getTomcat();
	}

	@Override
	protected void handleExceptionCausedByBlockedPort(RuntimeException ex, int blockedPort) {
		assertThat(ex).isInstanceOf(ConnectorStartFailedException.class);
		assertThat(((ConnectorStartFailedException) ex).getPort()).isEqualTo(blockedPort);
	}

}
