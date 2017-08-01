package io.opentracing.contrib.spring.web.interceptor;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import javax.servlet.http.HttpServletRequest;

import org.junit.Test;
import org.mockito.Mockito;

import io.opentracing.SpanContext;
import io.opentracing.contrib.web.servlet.filter.TracingFilter;

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
        interceptor.afterCompletion(request, null, null, null);
    }

}
