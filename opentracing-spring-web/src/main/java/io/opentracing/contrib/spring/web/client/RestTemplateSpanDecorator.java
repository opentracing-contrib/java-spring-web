package io.opentracing.contrib.spring.web.client;

import io.opentracing.BaseSpan;
import io.opentracing.tag.Tags;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpResponse;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Decorate span by adding tags/logs or operation name change.
 *
 * <p>Do not finish span or throw any exceptions!
 *
 * @author Pavol Loffay
 */
public interface RestTemplateSpanDecorator {

    /**
     * Decorate span before before request is executed, e.g. before
     * {@link org.springframework.http.client.ClientHttpRequestInterceptor#intercept(HttpRequest, byte[], ClientHttpRequestExecution)}
     * is called.
     *
     * @param request request
     * @param span client span
     */
    void onRequest(HttpRequest request, BaseSpan<?> span);

    /**
     * Decorate span after request is done, e.g. after
     * {@link org.springframework.http.client.ClientHttpRequestInterceptor#intercept(HttpRequest, byte[], ClientHttpRequestExecution)}
     * is called
     *
     * @param request request
     * @param response response
     * @param span span
     */
    void onResponse(HttpRequest request, ClientHttpResponse response, BaseSpan<?> span);

    /**
     * Decorate span when exception is thrown during request processing, e.g. during
     * {@link org.springframework.http.client.ClientHttpRequestInterceptor#intercept(HttpRequest, byte[], ClientHttpRequestExecution)}
     * is processing.
     *
     * @param request request
     * @param ex exception
     * @param span span
     */
    void onError(HttpRequest request,  Throwable ex, BaseSpan<?> span);

    /**
     * This decorator adds set of standard tags to the span.
     */
    class StandardTags implements RestTemplateSpanDecorator {
        private static final Logger log = Logger.getLogger(StandardTags.class.getName());

        public static String COMPONENT_NAME = "java-spring-rest-template";

        @Override
        public void onRequest(HttpRequest request, BaseSpan<?> span) {
            Tags.COMPONENT.set(span, COMPONENT_NAME);
            // this can be sometimes only path e.g. "/foo"
            Tags.HTTP_URL.set(span, request.getURI().toString());
            Tags.HTTP_METHOD.set(span, request.getMethod().toString());

            if (request.getURI().getPort() != -1) {
                Tags.PEER_PORT.set(span, request.getURI().getPort());
            }
        }

        @Override
        public void onResponse(HttpRequest httpRequest, ClientHttpResponse response, BaseSpan<?> span) {
            try {
                Tags.HTTP_STATUS.set(span, response.getRawStatusCode());
            } catch (IOException e) {
                log.severe("Could not get HTTP status code");
            }
        }

        @Override
        public void onError(HttpRequest httpRequest, Throwable ex, BaseSpan<?> span) {
            Tags.ERROR.set(span, Boolean.TRUE);
            span.log(errorLogs(ex));
        }

        public static Map<String, Object> errorLogs(Throwable ex) {
            Map<String, Object> errorLogs = new HashMap<>(2);
            errorLogs.put("event", Tags.ERROR.getKey());
            errorLogs.put("error.object", ex);
            return errorLogs;
        }
    }
}
