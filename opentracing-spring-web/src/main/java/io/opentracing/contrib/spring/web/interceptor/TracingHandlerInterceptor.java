package io.opentracing.contrib.spring.web.interceptor;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import io.opentracing.References;
import io.opentracing.Span;
import io.opentracing.SpanContext;
import io.opentracing.Tracer;
import io.opentracing.contrib.web.servlet.filter.SpanWrapper;
import io.opentracing.contrib.web.servlet.filter.TracingFilter;

/**
 * Tracing handler interceptor for spring web. It creates a new span for an incoming request.
 * This handler depends on {@link TracingFilter}. Both classes have to be properly configured.
 *
 * <p>HTTP tags and logged errors are added in {@link TracingFilter}. This interceptor adds only
 * spring related logs (handler class/method).
 *
 * <p>Server span context can be accessed via {@link #serverSpanContext(ServletRequest)}.
 *
 * @author Pavol Loffay
 */
public class TracingHandlerInterceptor extends HandlerInterceptorAdapter {
    private static final Logger log = Logger.getLogger(TracingHandlerInterceptor.class.getName());

    static final String AFTER_CONCURRENT_HANDLING_STARTED = TracingHandlerInterceptor.class.getName() +
            ".afterConcurrentHandlingStarted";

    static final String CONTINUED_SERVER_SPAN = TracingHandlerInterceptor.class.getName() + ".continuedServerSpan";

    private Tracer tracer;
    private List<SpanDecorator> decorators;

    /**
     * @param tracer tracer
     * @param decorators span decorators
     */
    @Autowired
    public TracingHandlerInterceptor(Tracer tracer, List<SpanDecorator> decorators) {
        this.tracer = tracer;
        this.decorators = new ArrayList<>(decorators);
    }

    @Override
    public boolean preHandle(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, Object handler)
            throws Exception {

        Object spanAttribute = httpServletRequest.getAttribute(TracingFilter.SERVER_SPAN_WRAPPER);
        if (!(spanAttribute instanceof SpanWrapper)) {
            log.severe("Span not found! TracingFilter is not registered!?");
            return true;
        }

        // skip preHandler after afterConcurrentHandlingStarted
        if (httpServletRequest.getAttribute(AFTER_CONCURRENT_HANDLING_STARTED) != null) {
            httpServletRequest.removeAttribute(AFTER_CONCURRENT_HANDLING_STARTED);
            return true;
        }

        SpanWrapper filterSpan = (SpanWrapper) spanAttribute;

        Span serverSpan = filterSpan.get();

        /**
         * If span in filter is finished create new span. However we need to check if there is already created span
         * for this use case.
         */
        if (filterSpan.isFinished()) {
            Object continuedSpanAttribute = httpServletRequest.getAttribute(CONTINUED_SERVER_SPAN);
            if (continuedSpanAttribute == null ||
                    (continuedSpanAttribute instanceof InterceptorSpanWrapper &&
                            ((InterceptorSpanWrapper)continuedSpanAttribute).isFinished())) {
                serverSpan = tracer.buildSpan(httpServletRequest.getMethod())
                        .addReference(References.FOLLOWS_FROM, filterSpan.get().context())
                        .start();
                httpServletRequest.setAttribute(CONTINUED_SERVER_SPAN, new InterceptorSpanWrapper(serverSpan, handler));
                // override server span context so local span in controllers will be child of it
                httpServletRequest.setAttribute(TracingFilter.SERVER_SPAN_CONTEXT, serverSpan.context());
            }
        }

        for (SpanDecorator decorator: decorators) {
            decorator.onPreHandle(httpServletRequest, handler, serverSpan);
        }

        return true;
    }

    @Override
    public void afterConcurrentHandlingStarted(HttpServletRequest request, HttpServletResponse response,
                                               Object handler) throws Exception {
        request.setAttribute(AFTER_CONCURRENT_HANDLING_STARTED, Boolean.TRUE);
    }

    @Override
    public void afterCompletion(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse,
                                Object handler, Exception ex) throws Exception {

        Object spanAttribute = httpServletRequest.getAttribute(TracingFilter.SERVER_SPAN_WRAPPER);
        if (!(spanAttribute instanceof SpanWrapper)) {
            log.severe("Span not found! TracingFilter is not registered!?");
            return;
        }

        SpanWrapper filterSpan = (SpanWrapper) spanAttribute;

        Span serverSpan = filterSpan.get();
        if (filterSpan.isFinished()) {
            serverSpan = ((InterceptorSpanWrapper)httpServletRequest.getAttribute(CONTINUED_SERVER_SPAN)).get();
        }

        for (SpanDecorator decorator: decorators) {
            decorator.onAfterCompletion(httpServletRequest, httpServletResponse, handler, ex, serverSpan);
        }

        if (filterSpan.isFinished()) {
            ((InterceptorSpanWrapper)httpServletRequest.getAttribute(CONTINUED_SERVER_SPAN)).finish(handler);
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

    /**
     * Get context of server span.
     *
     * @param servletRequest request
     * @return span context of server span
     */
    public static SpanContext serverSpanContext(ServletRequest servletRequest) {
        return (SpanContext) servletRequest.getAttribute(TracingFilter.SERVER_SPAN_CONTEXT);
    }
}
