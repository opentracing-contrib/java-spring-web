package io.opentracing.contrib.spring.web.interceptor;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import javax.servlet.http.HttpServletRequest;

import org.junit.Test;
import org.mockito.Mockito;

import io.opentracing.contrib.web.servlet.filter.TracingFilter;

public class TracingHandlerInterceptorTest {

    @Test
    public void testHasSpanStarted() {
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        Mockito.when(request.getAttribute(TracingFilter.SERVER_SPAN_CONTEXT)).thenReturn("");
        assertTrue(TracingHandlerInterceptor.hasSpanStarted(request));
    }

    @Test
    public void testHasSpanNotStarted() {
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        Mockito.when(request.getAttribute(TracingFilter.SERVER_SPAN_CONTEXT)).thenReturn(null);
        assertFalse(TracingHandlerInterceptor.hasSpanStarted(request));
    }

    @Test
    public void testPreHandleNoSpan() throws Exception {
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        Mockito.when(request.getAttribute(TracingFilter.SERVER_SPAN_CONTEXT)).thenReturn(null);

        TracingHandlerInterceptor interceptor = new TracingHandlerInterceptor(null);
        assertTrue(interceptor.preHandle(request, null, null));
    }

    @Test
    public void testAfterCompletionNoSpan() throws Exception {
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        Mockito.when(request.getAttribute(TracingFilter.SERVER_SPAN_CONTEXT)).thenReturn(null);

        TracingHandlerInterceptor interceptor = new TracingHandlerInterceptor(null);
        interceptor.afterCompletion(request, null, null, null);
    }

}
