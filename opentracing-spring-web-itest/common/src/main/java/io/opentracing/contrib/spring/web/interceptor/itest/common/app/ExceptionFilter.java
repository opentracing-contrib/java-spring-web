package io.opentracing.contrib.spring.web.interceptor.itest.common.app;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

/**
 * @author Pavol Loffay
 */
public class ExceptionFilter implements Filter {

    public static final String EXCEPTION_URL = "/filterException";
    public static final String EXCEPTION_MESSAGE = "filterMessage";

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {}

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpServletRequest = (HttpServletRequest) request;

        if (httpServletRequest.getRequestURL().toString().contains(EXCEPTION_URL)) {
            throw new RuntimeException(EXCEPTION_MESSAGE);
        }

        chain.doFilter(request, response);
    }

    @Override
    public void destroy() {}
}
