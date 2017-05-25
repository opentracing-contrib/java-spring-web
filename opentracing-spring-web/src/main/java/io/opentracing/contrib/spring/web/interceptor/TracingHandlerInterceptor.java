package io.opentracing.contrib.spring.web.interceptor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import io.opentracing.ActiveSpan;
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

        ActiveSpan serverSpan = tracer.activeSpan();
        if (serverSpan == null) {
            return true;
        }

        httpServletRequest.setAttribute(ActiveSpan.Continuation.class.getName() + handler.toString(),
                serverSpan.capture());

        for (HandlerInterceptorSpanDecorator decorator : decorators) {
            decorator.onPreHandle(httpServletRequest, handler, serverSpan);
        }

        return true;
    }

    @Override
    public void afterConcurrentHandlingStarted(HttpServletRequest request, HttpServletResponse response,
                                               Object handler) throws Exception {
    }

    @Override
    public void afterCompletion(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse,
                                Object handler, Exception ex) throws Exception {
        ActiveSpan serverSpan = null;
        ActiveSpan.Continuation cont = (ActiveSpan.Continuation)
                httpServletRequest.getAttribute(ActiveSpan.Continuation.class.getName() + handler.toString());

        if (cont != null) {
            serverSpan = cont.activate();
        }

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
