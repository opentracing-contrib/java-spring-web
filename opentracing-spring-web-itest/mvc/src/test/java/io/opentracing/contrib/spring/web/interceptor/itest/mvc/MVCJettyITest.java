package io.opentracing.contrib.spring.web.interceptor.itest.mvc;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.awaitility.Awaitility;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.webapp.WebAppContext;
import org.hamcrest.core.IsEqual;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.client.RestTemplateBuilder;

import io.opentracing.contrib.spring.web.interceptor.itest.common.AbstractBaseITests;
import io.opentracing.contrib.spring.web.interceptor.itest.common.app.ExceptionFilter;
import io.opentracing.contrib.spring.web.interceptor.itest.common.app.TestController;
import io.opentracing.contrib.spring.web.interceptor.itest.common.app.TracingBeansConfiguration;
import io.opentracing.mock.MockSpan;
import io.opentracing.tag.Tags;

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

    @Test
    public void testFilterException() throws Exception {
        {
            getRestTemplate().getForEntity(ExceptionFilter.EXCEPTION_URL, String.class);
            Awaitility.await().until(reportedSpansSize(), IsEqual.equalTo(1));
        }
        List<MockSpan> mockSpans = TracingBeansConfiguration.mockTracer.finishedSpans();
        Assert.assertEquals(1, mockSpans.size());
        assertOnErrors(mockSpans);

        MockSpan span = mockSpans.get(0);
        Assert.assertEquals("GET", span.operationName());

        Assert.assertEquals(6, span.tags().size());
        Assert.assertEquals(Tags.SPAN_KIND_SERVER, span.tags().get(Tags.SPAN_KIND.getKey()));
        Assert.assertEquals("GET", span.tags().get(Tags.HTTP_METHOD.getKey()));
        Assert.assertEquals(getUrl(ExceptionFilter.EXCEPTION_URL),
                span.tags().get(Tags.HTTP_URL.getKey()));
        Assert.assertEquals(500, span.tags().get(Tags.HTTP_STATUS.getKey()));
        Assert.assertNotNull(span.tags().get(Tags.COMPONENT.getKey()));
        Assert.assertEquals(Boolean.TRUE, span.tags().get(Tags.ERROR.getKey()));

//        request is not hitting controller
        assertLogEvents(span.logEntries(), Arrays.asList("error"));
//        error logs
        Assert.assertEquals(3, span.logEntries().get(0).fields().size());
        Assert.assertEquals(Tags.ERROR.getKey(), span.logEntries().get(0).fields().get("event"));
        Assert.assertNotNull(span.logEntries().get(0).fields().get("stack"));
        Assert.assertEquals(ExceptionFilter.EXCEPTION_MESSAGE,
                span.logEntries().get(0).fields().get("message"));
    }

    @Test
    public void testSecuredURLUnAuthorized() throws Exception {
        {
            getRestTemplate().getForEntity("/secured", String.class);
            Awaitility.await().until(reportedSpansSize(), IsEqual.equalTo(1));
        }
        List<MockSpan> mockSpans = TracingBeansConfiguration.mockTracer.finishedSpans();
        Assert.assertEquals(1, mockSpans.size());
        assertOnErrors(mockSpans);

        MockSpan span = mockSpans.get(0);
        Assert.assertEquals("GET", span.operationName());
        Assert.assertEquals(5, span.tags().size());
        Assert.assertEquals(Tags.SPAN_KIND_SERVER, span.tags().get(Tags.SPAN_KIND.getKey()));
        Assert.assertEquals("GET", span.tags().get(Tags.HTTP_METHOD.getKey()));
        Assert.assertEquals(getUrl("/secured"), span.tags().get(Tags.HTTP_URL.getKey()));
        Assert.assertEquals(401, span.tags().get(Tags.HTTP_STATUS.getKey()));
        Assert.assertNotNull(span.tags().get(Tags.COMPONENT.getKey()));

//        request does not hit any controller
        assertLogEvents(span.logEntries(), Collections.<String>emptyList());
    }


    @Test
    public void testNoURLMapping() {
        {
            getRestTemplate().getForEntity("/nouUrlMapping", String.class);
            Awaitility.await().until(reportedSpansSize(), IsEqual.equalTo(1));
        }
        List<MockSpan> mockSpans = TracingBeansConfiguration.mockTracer.finishedSpans();
        Assert.assertEquals(1, mockSpans.size());
        assertOnErrors(mockSpans);

        MockSpan span = mockSpans.get(0);
        Assert.assertEquals("GET", span.operationName());
        Assert.assertEquals(404, span.tags().get(Tags.HTTP_STATUS.getKey()));

        assertLogEvents(span.logEntries(), Collections.<String>emptyList());
    }

    @Test
    public void testControllerMappedException() throws Exception {
        {
            getRestTemplate().getForEntity("/mappedException", String.class);
            Awaitility.await().until(reportedSpansSize(), IsEqual.equalTo(1));
        }
        List<MockSpan> mockSpans = TracingBeansConfiguration.mockTracer.finishedSpans();
        Assert.assertEquals(1, mockSpans.size());
        assertOnErrors(mockSpans);

        MockSpan span = mockSpans.get(0);
        Assert.assertEquals("mappedException", span.operationName());

        Assert.assertEquals(5, span.tags().size());
        Assert.assertEquals(Tags.SPAN_KIND_SERVER, span.tags().get(Tags.SPAN_KIND.getKey()));
        Assert.assertEquals("GET", span.tags().get(Tags.HTTP_METHOD.getKey()));
        Assert.assertEquals(getUrl("/mappedException"), span.tags().get(Tags.HTTP_URL.getKey()));
        Assert.assertEquals(409, span.tags().get(Tags.HTTP_STATUS.getKey()));
        Assert.assertNotNull(span.tags().get(Tags.COMPONENT.getKey()));

        assertLogEvents(span.logEntries(), Arrays.asList("preHandle", "afterCompletion"));
    }

    @Test
    public void testControllerException() throws Exception {
        {
            getRestTemplate().getForEntity("/exception", String.class);
            Awaitility.await().until(reportedSpansSize(), IsEqual.equalTo(1));
        }
        List<MockSpan> mockSpans = TracingBeansConfiguration.mockTracer.finishedSpans();
        Assert.assertEquals(1, mockSpans.size());
        assertOnErrors(mockSpans);

        MockSpan span = mockSpans.get(0);

        Assert.assertEquals("exception", span.operationName());
        Assert.assertEquals(6, span.tags().size());
        Assert.assertEquals(Tags.SPAN_KIND_SERVER, span.tags().get(Tags.SPAN_KIND.getKey()));
        Assert.assertEquals("GET", span.tags().get(Tags.HTTP_METHOD.getKey()));
        Assert.assertEquals(getUrl("/exception"), span.tags().get(Tags.HTTP_URL.getKey()));
        Assert.assertEquals(500, span.tags().get(Tags.HTTP_STATUS.getKey()));
        Assert.assertNotNull(span.tags().get(Tags.COMPONENT.getKey()));
        Assert.assertEquals(Boolean.TRUE, span.tags().get(Tags.ERROR.getKey()));

        assertLogEvents(span.logEntries(), Arrays.asList("preHandle", "afterCompletion", "error"));
//        error log
        Assert.assertEquals(3, span.logEntries().get(2).fields().size());
        Assert.assertEquals(Tags.ERROR.getKey(), span.logEntries().get(2).fields().get("event"));
        Assert.assertEquals(TestController.EXCEPTION_MESSAGE,
                span.logEntries().get(2).fields().get("message"));
        Assert.assertNotNull(span.logEntries().get(2).fields().get("stack"));
    }
}
