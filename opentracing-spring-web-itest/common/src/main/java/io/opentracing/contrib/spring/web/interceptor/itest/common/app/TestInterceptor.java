package io.opentracing.contrib.spring.web.interceptor.itest.common.app;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

/**
 * @author Pavol Loffay
 */
public class TestInterceptor extends HandlerInterceptorAdapter {

    public static final String EXCEPTION_MESSAGE = "interceptorException";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        if(request.getRequestURL().toString().contains("/sync")) {
            // stop request
            return false;
        }
        return true;
    }

    @Override
    public void afterCompletion(
            HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex)
            throws Exception {

        if(request.getRequestURL().toString().contains("/sync")) {
            throw new Exception(EXCEPTION_MESSAGE);
        }
    }
}
