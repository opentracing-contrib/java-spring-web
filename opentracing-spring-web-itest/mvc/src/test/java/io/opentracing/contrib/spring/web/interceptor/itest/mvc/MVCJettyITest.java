package io.opentracing.contrib.spring.web.interceptor.itest.mvc;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.webapp.WebAppContext;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.client.RestTemplateBuilder;

import io.opentracing.contrib.spring.web.interceptor.itest.common.AbstractBaseITests;

/**
 * @author Pavol Loffay
 */
public class MVCJettyITest extends AbstractBaseITests {
    protected static final String CONTEXT_PATH = "/tracing";

    static Server jettyServer;
    // jetty starts on random port
    static int serverPort;

    static TestRestTemplate testRestTemplate;

    @BeforeClass
    public static void beforeClass() throws Exception {
        jettyServer = new Server(0);

        WebAppContext webApp = new WebAppContext();
        webApp.setServer(jettyServer);
        webApp.setContextPath(CONTEXT_PATH);
        webApp.setWar("src/test/webapp");

        jettyServer.setHandler(webApp);
        jettyServer.start();
        serverPort = ((ServerConnector)jettyServer.getConnectors()[0]).getLocalPort();

        testRestTemplate = new TestRestTemplate(new RestTemplateBuilder()
                .rootUri("http://localhost:" + serverPort + CONTEXT_PATH));
    }

    @AfterClass
    public static void afterTest() throws Exception {
        jettyServer.stop();
        jettyServer.join();
    }

    @Override
    protected String getUrl(String path) {
        return "http://localhost:" + serverPort + CONTEXT_PATH + path;
    }

    @Override
    protected TestRestTemplate getRestTemplate() {
        return testRestTemplate;
    }

}
