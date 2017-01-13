package io.opentracing.contrib.spring.web.interceptor;

import io.opentracing.Span;
import io.opentracing.contrib.web.servlet.filter.SpanWrapper;

/**
 * Wrapper used to store span in request attributes.
 *
 * @author Pavol Loffay
 */
class InterceptorSpanWrapper extends SpanWrapper {

    private Object handler;

    InterceptorSpanWrapper(Span span, Object handler) {
        super(span);
        this.handler = handler;
    }

    /**
     * @param handler spring handler object. Span is finished only if the handler from constructor is the same.
     */
    void finish(Object handler) {
        if (handler.equals(this.handler)) {
            super.finish();
        }
    }
}
