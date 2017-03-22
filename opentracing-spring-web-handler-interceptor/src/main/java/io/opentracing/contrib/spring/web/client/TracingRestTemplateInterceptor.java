package io.opentracing.contrib.spring.web.client;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

import io.opentracing.Span;
import io.opentracing.Tracer;
import io.opentracing.propagation.Format;
import io.opentracing.tag.Tags;

/**
 * OpenTracing Spring RestTemplate integration.
 * This interceptor creates tracing data for all outgoing requests.
 *
 * @author Pavol Loffay
 */
public class TracingRestTemplateInterceptor implements ClientHttpRequestInterceptor {
    private static final Logger log = Logger.getLogger(TracingRestTemplateInterceptor.class.getName());

    private Tracer tracer;
    private List<RestTemplateSpanDecorator> spanDecorators;

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

        Tracer.SpanBuilder spanBuilder = tracer.buildSpan(httpRequest.getMethod().toString())
                .withTag(Tags.SPAN_KIND.getKey(), Tags.SPAN_KIND_CLIENT);

        // TODO add parent span via in-process propagation
//        if (parent-is-present) {
//            spanBuilder.asChildOf(parent);
//        }

        Span span = spanBuilder.start();
        ClientHttpResponse httpResponse;

        try {
            tracer.inject(span.context(), Format.Builtin.HTTP_HEADERS,
                    new HttpHeadersCarrier(httpRequest.getHeaders()));

            for (RestTemplateSpanDecorator spanDecorator : spanDecorators) {
                try {
                    spanDecorator.onRequest(httpRequest, span);
                } catch (RuntimeException ex) {
                    log.log(Level.SEVERE, "Exception during decorating span", ex);
                }
            }

            try {
                httpResponse = execution.execute(httpRequest, body);
            } catch (Exception ex) {
                for (RestTemplateSpanDecorator spanDecorator : spanDecorators) {
                    try {
                        spanDecorator.onError(httpRequest, ex, span);
                    } catch (RuntimeException exDecorator) {
                        log.log(Level.SEVERE, "Exception during decorating span", exDecorator);
                    }
                }
                throw ex;
            }

            for (RestTemplateSpanDecorator spanDecorator : spanDecorators) {
                try {
                    spanDecorator.onResponse(httpRequest, httpResponse, span);
                } catch (RuntimeException ex) {
                    log.log(Level.SEVERE, "Exception during decorating span", ex);
                }
            }
        } finally {
            span.finish();
        }

        return httpResponse;
    }
}
