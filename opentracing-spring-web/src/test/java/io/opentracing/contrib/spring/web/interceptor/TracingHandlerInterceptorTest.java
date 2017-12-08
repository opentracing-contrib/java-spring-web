package io.opentracing.contrib.spring.web.interceptor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import javax.servlet.http.HttpServletRequest;

import io.opentracing.mock.MockSpan;
import io.opentracing.mock.MockTracer;
import io.opentracing.util.ThreadLocalActiveSpanSource;
import org.junit.Test;
import org.mockito.Mockito;

import io.opentracing.SpanContext;
import io.opentracing.contrib.web.servlet.filter.TracingFilter;
import org.springframework.mock.web.MockHttpServletRequest;

public class TracingHandlerInterceptorTest {

    @Test
    public void testIsTraced() {
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        Mockito.when(request.getAttribute(TracingFilter.SERVER_SPAN_CONTEXT)).thenReturn(Mockito.mock(SpanContext.class));
        assertTrue(TracingHandlerInterceptor.isTraced(request));
    }

    @Test
    public void testIsNotTraced() {
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        Mockito.when(request.getAttribute(TracingFilter.SERVER_SPAN_CONTEXT)).thenReturn(null);
        assertFalse(TracingHandlerInterceptor.isTraced(request));
    }

    @Test
    public void testNoSpan() throws Exception {
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        Mockito.when(request.getAttribute(TracingFilter.SERVER_SPAN_CONTEXT)).thenReturn(null);

        TracingHandlerInterceptor interceptor = new TracingHandlerInterceptor(null);
        assertTrue(interceptor.preHandle(request, null, null));
        interceptor.afterConcurrentHandlingStarted(request, null, null);
        interceptor.afterCompletion(request, null, null, null);
    }

    @Test
    public void testErrorTagSet() throws Exception {
        MockTracer tracer = new MockTracer(new ThreadLocalActiveSpanSource());
        HttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute(TracingFilter.SERVER_SPAN_CONTEXT, new MockSpan.MockContext(1, 2, null));

        TracingHandlerInterceptor interceptor = new TracingHandlerInterceptor(tracer);
        interceptor.preHandle(request, null, Mockito.mock(Object.class));
        interceptor.afterCompletion(request, null, null, new Exception("foo"));

        assertEquals(1, tracer.finishedSpans().size());
        assertEquals(true, tracer.finishedSpans().get(0).tags().get("error"));
    }
}
