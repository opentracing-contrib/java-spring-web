package io.opentracing.contrib.spring.web.interceptor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import io.opentracing.ActiveSpan;
import io.opentracing.References;
import io.opentracing.SpanContext;
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
        ActiveSpan serverSpan = null;

        if (httpServletRequest.getAttribute(ActiveSpan.Continuation.class.getName()) != null) {
            ActiveSpan.Continuation cont = (ActiveSpan.Continuation)
                    httpServletRequest.getAttribute(ActiveSpan.Continuation.class.getName());
            // Clear attribute to make clear that the continuation has been used
            httpServletRequest.setAttribute(ActiveSpan.Continuation.class.getName(), null);
            serverSpan = cont.activate();
        } else if (tracer.activeSpan() != null) {
            serverSpan = tracer.activeSpan().capture().activate();
        } else if (httpServletRequest.getAttribute(TracingFilter.SERVER_SPAN_CONTEXT) != null) {
            serverSpan = tracer.buildSpan(httpServletRequest.getMethod())
                    .addReference(References.FOLLOWS_FROM,
                            (SpanContext)httpServletRequest.getAttribute(TracingFilter.SERVER_SPAN_CONTEXT))
                    .startActive();
        } else {
            return true;
        }

        for (HandlerInterceptorSpanDecorator decorator : decorators) {
            decorator.onPreHandle(httpServletRequest, handler, serverSpan);
        }

        // Create another continuation to pass to next handler in the chain
        httpServletRequest.setAttribute(ActiveSpan.Continuation.class.getName(),
                serverSpan.capture());

        return true;
    }

    @Override
    public void afterConcurrentHandlingStarted(
            HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, Object handler)
            throws Exception {
        ActiveSpan serverSpan = tracer.activeSpan();
        for (HandlerInterceptorSpanDecorator decorator : decorators) {
            decorator.onAfterConcurrentHandlingStarted(httpServletRequest, httpServletResponse, handler, serverSpan);
        }
        serverSpan.deactivate();
    }

    @Override
    public void afterCompletion(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse,
                                Object handler, Exception ex) throws Exception {
        if (httpServletRequest.getAttribute(ActiveSpan.Continuation.class.getName()) != null) {
            ActiveSpan.Continuation cont = (ActiveSpan.Continuation)
                    httpServletRequest.getAttribute(ActiveSpan.Continuation.class.getName());
            // Clear unused continuation, to prevent span being unreported
            cont.activate().deactivate();
            httpServletRequest.setAttribute(ActiveSpan.Continuation.class.getName(), null);
        }

        ActiveSpan serverSpan = tracer.activeSpan();
        if (serverSpan != null) {
            for (HandlerInterceptorSpanDecorator decorator : decorators) {
                decorator.onAfterCompletion(httpServletRequest, httpServletResponse, handler, ex, serverSpan);
            }
            
            serverSpan.deactivate();
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

}
