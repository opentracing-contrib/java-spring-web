package io.opentracing.contrib.spring.web.client;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

import io.opentracing.Scope;
import io.opentracing.Tracer;
import io.opentracing.propagation.Format;
import io.opentracing.tag.Tags;
import io.opentracing.util.GlobalTracer;

/**
 * OpenTracing Spring RestTemplate integration.
 * This interceptor creates tracing data for all outgoing requests.
 *
 * @author Pavol Loffay
 */
public class TracingRestTemplateInterceptor implements ClientHttpRequestInterceptor {
    private static final Log log = LogFactory.getLog(TracingRestTemplateInterceptor.class);

    private Tracer tracer;
    private List<RestTemplateSpanDecorator> spanDecorators;

    public TracingRestTemplateInterceptor() {
        this(GlobalTracer.get(), Collections.<RestTemplateSpanDecorator>singletonList(
                new RestTemplateSpanDecorator.StandardTags()));
    }

    /**
     * @param tracer tracer
     */
    public TracingRestTemplateInterceptor(Tracer tracer) {
        this(tracer, Collections.<RestTemplateSpanDecorator>singletonList(
                new RestTemplateSpanDecorator.StandardTags()));
    }

    /**
     * @param tracer tracer
     * @param spanDecorators list of decorators
     */
    public TracingRestTemplateInterceptor(Tracer tracer, List<RestTemplateSpanDecorator> spanDecorators) {
        this.tracer = tracer;
        this.spanDecorators = new ArrayList<>(spanDecorators);
    }

    @Override
    public ClientHttpResponse intercept(HttpRequest httpRequest, byte[] body,
                                        ClientHttpRequestExecution execution) throws IOException {
        ClientHttpResponse httpResponse;

        try (Scope scope = tracer.buildSpan(httpRequest.getMethod().toString())
                .withTag(Tags.SPAN_KIND.getKey(), Tags.SPAN_KIND_CLIENT).startActive(true)) {
            tracer.inject(scope.span().context(), Format.Builtin.HTTP_HEADERS,
                    new HttpHeadersCarrier(httpRequest.getHeaders()));

            for (RestTemplateSpanDecorator spanDecorator : spanDecorators) {
                try {
                    spanDecorator.onRequest(httpRequest, scope.span());
                } catch (RuntimeException exDecorator) {
                    log.error("Exception during decorating span", exDecorator);
                }
            }

            try {
                httpResponse = execution.execute(httpRequest, body);
            } catch (Exception ex) {
                for (RestTemplateSpanDecorator spanDecorator : spanDecorators) {
                    try {
                        spanDecorator.onError(httpRequest, ex, scope.span());
                    } catch (RuntimeException exDecorator) {
                        log.error("Exception during decorating span", exDecorator);
                    }
                }
                throw ex;
            }

            for (RestTemplateSpanDecorator spanDecorator : spanDecorators) {
                try {
                    spanDecorator.onResponse(httpRequest, httpResponse, scope.span());
                } catch (RuntimeException exDecorator) {
                    log.error("Exception during decorating span", exDecorator);
                }
            }
        }

        return httpResponse;
    }
}
