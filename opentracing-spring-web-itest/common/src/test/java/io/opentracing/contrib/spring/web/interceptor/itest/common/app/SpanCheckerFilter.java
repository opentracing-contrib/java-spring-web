package io.opentracing.contrib.spring.web.interceptor.itest.common.app;

import io.opentracing.Span;
import io.opentracing.util.GlobalTracer;
import org.springframework.web.filter.GenericFilterBean;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * This ensure that when the {@link io.opentracing.contrib.web.servlet.filter.TracingFilter} has opened a span,
 * this span is still (and the same) when we return from the doFilter().
 *
 * @author Alexander Schwartz
 */
public class SpanCheckerFilter extends GenericFilterBean {

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) servletRequest;
        HttpServletResponse httpResponse = (HttpServletResponse) servletResponse;
        Span span = GlobalTracer.get().activeSpan();
        chain.doFilter(httpRequest, httpResponse);
        if (span != GlobalTracer.get().activeSpan()) {
            throw new RuntimeException("we should see the same span before and after");
        }
    }
}
