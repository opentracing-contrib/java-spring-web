package io.opentracing.contrib.spring.web.interceptor;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import io.opentracing.ActiveSpan;
import io.opentracing.References;
import io.opentracing.Tracer;
import io.opentracing.contrib.web.servlet.filter.TracingFilter;

/**
 * Tracing handler interceptor for spring web. It creates a new span for an incoming request.
 * This handler depends on {@link TracingFilter}. Both classes have to be properly configured.
 *
 * <p>HTTP tags and logged errors are added in {@link TracingFilter}. This interceptor adds only
 * spring related logs (handler class/method).
 *
 * @author Pavol Loffay
 */
public class TracingHandlerInterceptor extends HandlerInterceptorAdapter {

    private static final String ACTIVE_SPAN_STACK = TracingHandlerInterceptor.class.getName() + ".activeSpanStack";
    private static final String CONTINUATION_FROM_ASYNC_STARTED = TracingHandlerInterceptor.class.getName() + ".continuation";

    private Tracer tracer;
    private List<HandlerInterceptorSpanDecorator> decorators;

    /**
     * @param tracer
     */
    @Autowired
    public TracingHandlerInterceptor(Tracer tracer) {
        this(tracer, Collections.singletonList(HandlerInterceptorSpanDecorator.STANDARD_TAGS));
    }

    /**
     * @param tracer tracer
     * @param decorators span decorators
     */
    @Autowired
    public TracingHandlerInterceptor(Tracer tracer, List<HandlerInterceptorSpanDecorator> decorators) {
        this.tracer = tracer;
        this.decorators = new ArrayList<>(decorators);
    }

    @Override
    public boolean preHandle(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, Object handler)
            throws Exception {

        // exclude pattern, span is not started in filter
        if (httpServletRequest.getAttribute(TracingFilter.SERVER_SPAN_CONTEXT) == null) {
            return true;
        }

        /**
         * 1. check if there is an active span, it has been activated in servlet filter or in this interceptor (forward)
         * 2. if there is no active span then it can be handling of an async request or spring boot default error handling
         */
        ActiveSpan serverSpan = tracer.activeSpan();
        if (serverSpan != null) {
            serverSpan = serverSpan.capture().activate();
        } else if (httpServletRequest.getAttribute(CONTINUATION_FROM_ASYNC_STARTED) != null) {
            ActiveSpan.Continuation contd = (ActiveSpan.Continuation) httpServletRequest.getAttribute(CONTINUATION_FROM_ASYNC_STARTED);
            serverSpan = contd.activate();
            httpServletRequest.removeAttribute(CONTINUATION_FROM_ASYNC_STARTED);
        } else {
            // spring boot default error handling, executes interceptor after processing in the filter (ugly huh?)
            serverSpan = tracer.buildSpan(httpServletRequest.getMethod())
                    .addReference(References.FOLLOWS_FROM, TracingFilter.serverSpanContext(httpServletRequest))
                    .startActive();
        }

        for (HandlerInterceptorSpanDecorator decorator : decorators) {
            decorator.onPreHandle(httpServletRequest, handler, serverSpan);
        }

        Deque<ActiveSpan> activeSpanStack = getActiveSpanStack(httpServletRequest);
        activeSpanStack.push(serverSpan);
        return true;
    }

    @Override
    public void afterConcurrentHandlingStarted (
            HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, Object handler)
            throws Exception {

        Deque<ActiveSpan> activeSpanStack = getActiveSpanStack(httpServletRequest);
        ActiveSpan activeSpan = activeSpanStack.pop();

        for (HandlerInterceptorSpanDecorator decorator : decorators) {
            decorator.onAfterConcurrentHandlingStarted(httpServletRequest, httpServletResponse, handler, activeSpan);
        }

        activeSpan.deactivate();
        httpServletRequest.setAttribute(CONTINUATION_FROM_ASYNC_STARTED, activeSpan.capture());
    }

    @Override
    public void afterCompletion(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse,
                                Object handler, Exception ex) throws Exception {

        Deque<ActiveSpan> activeSpanStack = getActiveSpanStack(httpServletRequest);
        ActiveSpan activeSpan = activeSpanStack.pop();
        if (activeSpan != null) {
            for (HandlerInterceptorSpanDecorator decorator : decorators) {
                decorator.onAfterCompletion(httpServletRequest, httpServletResponse, handler, ex, activeSpan);
            }
            activeSpan.deactivate();
        }
    }

    /**
     * It checks whether a request should be traced or not.
     *
     * @param httpServletRequest request
     * @param httpServletResponse response
     * @param handler handler
     * @return whether request should be traced or not
     */
    protected boolean isTraced(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse,
                               Object handler) {
        return true;
    }

    private Deque<ActiveSpan> getActiveSpanStack(HttpServletRequest request) {
        Deque<ActiveSpan> stack = (Deque<ActiveSpan>) request.getAttribute(ACTIVE_SPAN_STACK);
        if (stack == null) {
            stack = new ArrayDeque<>();
            request.setAttribute(ACTIVE_SPAN_STACK, stack);
        }
        return stack;
    }
}
