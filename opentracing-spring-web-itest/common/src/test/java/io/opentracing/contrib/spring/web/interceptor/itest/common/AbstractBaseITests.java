package io.opentracing.contrib.spring.web.interceptor.itest.common;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;

import org.awaitility.Awaitility;
import org.awaitility.Duration;
import org.hamcrest.core.IsEqual;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;

import io.opentracing.contrib.spring.web.interceptor.HandlerInterceptorSpanDecorator;
import io.opentracing.contrib.spring.web.interceptor.itest.common.app.ExceptionFilter;
import io.opentracing.contrib.spring.web.interceptor.itest.common.app.TestController;
import io.opentracing.contrib.spring.web.interceptor.itest.common.app.TracingBeansConfiguration;
import io.opentracing.mock.MockSpan;
import io.opentracing.tag.Tags;

/**
 * @author Pavol Loffay
 */
public abstract class AbstractBaseITests {

    @BeforeClass
    public static void beforeClass() throws Exception {
        Awaitility.setDefaultTimeout(Duration.FIVE_SECONDS);
    }

    @Before
    public void beforeTest() {
        TracingBeansConfiguration.mockTracer.reset();
    }

    protected abstract String getUrl(String path);
    protected abstract TestRestTemplate getRestTemplate();


    @Test
    public void testSyncWithStandardTags() throws Exception {
        {
            getRestTemplate().getForEntity("/sync", String.class);
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
        Assert.assertEquals(getUrl("/sync"), span.tags().get(Tags.HTTP_URL.getKey()));
        Assert.assertEquals(200, span.tags().get(Tags.HTTP_STATUS.getKey()));
        Assert.assertNotNull(span.tags().get(Tags.COMPONENT.getKey()));

        assertLogEvents(span.logEntries(), Arrays.asList("preHandle", "afterCompletion"));
    }

    @Test
    public void testAsync() throws Exception {
        {
            getRestTemplate().getForEntity("/async", String.class);
            Awaitility.await().until(reportedSpansSize(), IsEqual.equalTo(1));
        }
        List<MockSpan> mockSpans = TracingBeansConfiguration.mockTracer.finishedSpans();
        Assert.assertEquals(1, mockSpans.size());
        assertOnErrors(mockSpans);

        MockSpan span = mockSpans.get(0);
        Assert.assertEquals("GET", span.operationName());
        assertLogEvents(span.logEntries(), Arrays.asList("preHandle", "afterCompletion"));
    }

    @Test
    public void testAsyncDeferred() throws Exception {
        {
            getRestTemplate().getForEntity("/asyncDeferred", String.class);
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
        Assert.assertEquals(getUrl("/asyncDeferred"), span.tags().get(Tags.HTTP_URL.getKey()));
        Assert.assertEquals(202, span.tags().get(Tags.HTTP_STATUS.getKey()));
        Assert.assertNotNull(span.tags().get(Tags.COMPONENT.getKey()));

        assertLogEvents(span.logEntries(), Arrays.asList("preHandle", "afterCompletion"));
    }

    @Test
    public void testContextPropagation() throws Exception {
        {
            HttpHeaders headers = new HttpHeaders();
            headers.set("spanid", "1");
            headers.set("traceid", "345");

            HttpEntity<String> entity = new HttpEntity<>(headers);

            getRestTemplate().exchange("/sync", HttpMethod.GET, entity, String.class);
            Awaitility.await().until(reportedSpansSize(), IsEqual.equalTo(1));
        }
        List<MockSpan> mockSpans = TracingBeansConfiguration.mockTracer.finishedSpans();
        Assert.assertEquals(1, mockSpans.size());
        assertOnErrors(mockSpans);

        MockSpan span = mockSpans.get(0);
        Assert.assertEquals(1, span.parentId());
        Assert.assertEquals(345, span.context().traceId());
    }

    @Test
    public void testControllerException() throws Exception {
        {
            getRestTemplate().getForEntity("/exception", String.class);
            Awaitility.await().until(reportedSpansSize(), IsEqual.equalTo(2));
        }
        List<MockSpan> mockSpans = TracingBeansConfiguration.mockTracer.finishedSpans();
        Assert.assertEquals(2, mockSpans.size());
        assertOnErrors(mockSpans);

        MockSpan span = mockSpans.get(0);
        Assert.assertEquals("GET", span.operationName());
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

        span = mockSpans.get(1);
        Assert.assertEquals(0, span.tags().size());
        Assert.assertEquals(mockSpans.get(0).context().spanId(), span.parentId());
        Assert.assertEquals(0, span.tags().size());
        assertLogEvents(span.logEntries(), Arrays.asList("preHandle", "afterCompletion"));
        Assert.assertEquals("BasicErrorController",
                span.logEntries().get(0).fields().get("handler.class_simple_name"));
    }

    @Test
    public void testControllerMappedException() throws Exception {
        {
            getRestTemplate().getForEntity("/mappedException", String.class);
            Awaitility.await().until(reportedSpansSize(), IsEqual.equalTo(2));
        }
        List<MockSpan> mockSpans = TracingBeansConfiguration.mockTracer.finishedSpans();
        Assert.assertEquals(2, mockSpans.size());
        assertOnErrors(mockSpans);

        MockSpan span = mockSpans.get(0);
        Assert.assertEquals("GET", span.operationName());

        Assert.assertEquals(5, span.tags().size());
        Assert.assertEquals(Tags.SPAN_KIND_SERVER, span.tags().get(Tags.SPAN_KIND.getKey()));
        Assert.assertEquals("GET", span.tags().get(Tags.HTTP_METHOD.getKey()));
        Assert.assertEquals(getUrl("/mappedException"), span.tags().get(Tags.HTTP_URL.getKey()));
        Assert.assertEquals(409, span.tags().get(Tags.HTTP_STATUS.getKey()));
        Assert.assertNotNull(span.tags().get(Tags.COMPONENT.getKey()));

        assertLogEvents(span.logEntries(), Arrays.asList("preHandle", "afterCompletion"));

        span = mockSpans.get(1);
        Assert.assertEquals(0, span.tags().size());
        Assert.assertEquals(mockSpans.get(0).context().spanId(), span.parentId());
        Assert.assertEquals(0, span.tags().size());
        assertLogEvents(span.logEntries(), Arrays.asList("preHandle", "afterCompletion"));
        Assert.assertEquals("BasicErrorController",
                span.logEntries().get(0).fields().get("handler.class_simple_name"));
    }

    @Test
    public void testFilterException() throws Exception {
        {
            getRestTemplate().getForEntity(ExceptionFilter.EXCEPTION_URL, String.class);
            Awaitility.await().until(reportedSpansSize(), IsEqual.equalTo(2));
        }
        List<MockSpan> mockSpans = TracingBeansConfiguration.mockTracer.finishedSpans();
        Assert.assertEquals(2, mockSpans.size());
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

        span = mockSpans.get(1);
        Assert.assertEquals(0, span.tags().size());
        Assert.assertEquals(mockSpans.get(0).context().spanId(), span.parentId());
        Assert.assertEquals(0, span.tags().size());
        assertLogEvents(span.logEntries(), Arrays.asList("preHandle", "afterCompletion"));
        Assert.assertEquals("BasicErrorController",
                span.logEntries().get(0).fields().get("handler.class_simple_name"));
    }

    @Test
    public void testNoURLMapping() {
        {
            getRestTemplate().getForEntity("/nouUrlMapping", String.class);
            Awaitility.await().until(reportedSpansSize(), IsEqual.equalTo(2));
        }
        List<MockSpan> mockSpans = TracingBeansConfiguration.mockTracer.finishedSpans();
        Assert.assertEquals(2, mockSpans.size());
        assertOnErrors(mockSpans);

        MockSpan span = mockSpans.get(0);
        Assert.assertEquals("GET", span.operationName());
        Assert.assertEquals(404, span.tags().get(Tags.HTTP_STATUS.getKey()));

        assertLogEvents(span.logEntries(), Collections.<String>emptyList());

        span = mockSpans.get(1);
        Assert.assertEquals(0, span.tags().size());
        Assert.assertEquals(mockSpans.get(0).context().spanId(), span.parentId());
        Assert.assertEquals(0, span.tags().size());
        assertLogEvents(span.logEntries(), Arrays.asList("preHandle", "afterCompletion"));
        Assert.assertEquals("BasicErrorController",
                span.logEntries().get(0).fields().get("handler.class_simple_name"));
    }

    @Test
    public void testSecuredURLUnAuthorized() throws Exception {
        {
            getRestTemplate().getForEntity("/secured", String.class);
            Awaitility.await().until(reportedSpansSize(), IsEqual.equalTo(2));
        }
        List<MockSpan> mockSpans = TracingBeansConfiguration.mockTracer.finishedSpans();
        Assert.assertEquals(2, mockSpans.size());
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

        span = mockSpans.get(1);
        Assert.assertEquals(0, span.tags().size());
        Assert.assertEquals(mockSpans.get(0).context().spanId(), span.parentId());
        Assert.assertEquals(0, span.tags().size());
        assertLogEvents(span.logEntries(), Arrays.asList("preHandle", "afterCompletion"));
        Assert.assertEquals("BasicErrorController",
                span.logEntries().get(0).fields().get("handler.class_simple_name"));
    }

    @Test
    public void testSecuredURLAuthorized() throws Exception {
        {
            getRestTemplate().withBasicAuth("user", "password").getForEntity("/secured", String.class);
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
        Assert.assertEquals(200, span.tags().get(Tags.HTTP_STATUS.getKey()));
        Assert.assertNotNull(span.tags().get(Tags.COMPONENT.getKey()));

        assertLogEvents(span.logEntries(), Arrays.asList("preHandle", "afterCompletion"));
    }

    @Test
    public void testWildcardMapping() {
        {
            getRestTemplate().getForEntity("/wildcard/param/44", String.class);
            Awaitility.await().until(reportedSpansSize(), IsEqual.equalTo(1));
        }
        List<MockSpan> mockSpans = TracingBeansConfiguration.mockTracer.finishedSpans();
        Assert.assertEquals(1, mockSpans.size());
        assertOnErrors(mockSpans);
    }

    @Test
    public void testRedirect() {
        {
            getRestTemplate().getForEntity("/redirect", String.class);
            Awaitility.await().until(reportedSpansSize(), IsEqual.equalTo(2));
        }
        List<MockSpan> mockSpans = TracingBeansConfiguration.mockTracer.finishedSpans();
        Assert.assertEquals(2, mockSpans.size());
        assertOnErrors(mockSpans);
    }

    @Test
    public void testForward() {
        {
            getRestTemplate().getForEntity("/forward", String.class);
            Awaitility.await().until(reportedSpansSize(), IsEqual.equalTo(1));
        }
        List<MockSpan> mockSpans = TracingBeansConfiguration.mockTracer.finishedSpans();
        Assert.assertEquals(1, mockSpans.size());
        assertOnErrors(mockSpans);

        MockSpan mockSpan = mockSpans.get(0);
        assertLogEvents(mockSpan.logEntries(), Arrays.asList("preHandle", "preHandle", "afterCompletion",
                "afterCompletion"));

        Assert.assertEquals("forward",
                mockSpan.logEntries().get(0).fields().get(HandlerInterceptorSpanDecorator.HandlerUtils.HANDLER_METHOD_NAME));
        Assert.assertEquals("sync",
                mockSpan.logEntries().get(1).fields().get(HandlerInterceptorSpanDecorator.HandlerUtils.HANDLER_METHOD_NAME));
        Assert.assertTrue(mockSpan.logEntries().get(2).fields().get(HandlerInterceptorSpanDecorator.HandlerUtils.HANDLER)
                .toString().contains("sync"));
        Assert.assertTrue(mockSpan.logEntries().get(3).fields().get(HandlerInterceptorSpanDecorator.HandlerUtils.HANDLER)
                .toString().contains("forward"));
    }

    @Test
    public void testLocalSpan() {
        {
            getRestTemplate().getForEntity("/localSpan", String.class);
            Awaitility.await().until(reportedSpansSize(), IsEqual.equalTo(2));
        }
        List<MockSpan> mockSpans = TracingBeansConfiguration.mockTracer.finishedSpans();
        Assert.assertEquals(2, mockSpans.size());
        assertOnErrors(mockSpans);

        Assert.assertEquals(mockSpans.get(0).context().traceId(), mockSpans.get(1).context().traceId());
        Assert.assertEquals(mockSpans.get(0).parentId(), mockSpans.get(1).context().spanId());
    }

    @Test
    public void testView() {
        {
            getRestTemplate().getForEntity("/view", String.class);
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
        Assert.assertEquals(getUrl("/view"), span.tags().get(Tags.HTTP_URL.getKey()));
        Assert.assertEquals(200, span.tags().get(Tags.HTTP_STATUS.getKey()));
        Assert.assertNotNull(span.tags().get(Tags.COMPONENT.getKey()));

        assertLogEvents(span.logEntries(), Arrays.asList("preHandle", "afterCompletion"));
    }

    @Test
    public void testControllerView() {
        {
            getRestTemplate().getForEntity("/controllerView", String.class);
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
        Assert.assertEquals(getUrl("/controllerView"), span.tags().get(Tags.HTTP_URL.getKey()));
        Assert.assertEquals(200, span.tags().get(Tags.HTTP_STATUS.getKey()));
        Assert.assertNotNull(span.tags().get(Tags.COMPONENT.getKey()));

        assertLogEvents(span.logEntries(), Arrays.asList("preHandle", "afterCompletion"));
    }

    @Test
    public void testExcludePattern() throws InterruptedException {
        {
            getRestTemplate().getForEntity("/health", String.class);
            Thread.sleep(100); // wait some time to be sure that span is not created
        }

        Assert.assertTrue(TracingBeansConfiguration.mockTracer.finishedSpans().isEmpty());
    }

    public static Callable<Integer> reportedSpansSize() {
        return new Callable<Integer>() {
            @Override
            public Integer call() throws Exception {
                return TracingBeansConfiguration.mockTracer.finishedSpans().size();
            }
        };
    }

    public static void assertLogEvents(List<MockSpan.LogEntry> logs, List<String> events) {
        if (logs.size() != events.size()) {
            Assert.fail(String.format("Logs count does not match: expected %d, actual %d", events.size(), logs.size()));
        }

        for (int i = 0; i < logs.size(); i++) {
            Assert.assertEquals(events.get(i), logs.get(i).fields().get("event"));
        }
    }

    public static void assertOnErrors(List<MockSpan> spans) {
        for (MockSpan mockSpan: spans) {
            Assert.assertEquals(mockSpan.generatedErrors().toString(), 0, mockSpan.generatedErrors().size());
        }
    }
}
