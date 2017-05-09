package io.opentracing.contrib.spring.web.client;

import io.opentracing.Span;
import io.opentracing.Tracer;
import io.opentracing.contrib.spanmanager.DefaultSpanManager;
import io.opentracing.contrib.spanmanager.SpanManager;
import io.opentracing.propagation.Format;
import io.opentracing.tag.Tags;
import io.opentracing.util.GlobalTracer;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.AsyncClientHttpRequestExecution;
import org.springframework.http.client.AsyncClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.concurrent.ListenableFutureCallback;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Pavol Loffay
 */
public class TracingAsyncRestTemplateInterceptor implements AsyncClientHttpRequestInterceptor {
    private static final Logger log = Logger.getLogger(TracingAsyncRestTemplateInterceptor.class.getName());

    private Tracer tracer;
    private SpanManager spanManager = DefaultSpanManager.getInstance();
    private List<RestTemplateSpanDecorator> spanDecorators;

    public TracingAsyncRestTemplateInterceptor() {
        this(GlobalTracer.get());
    }

    public TracingAsyncRestTemplateInterceptor(Tracer tracer) {
        this.tracer = tracer;
        this.spanDecorators = Collections.<RestTemplateSpanDecorator>singletonList(new RestTemplateSpanDecorator.StandardTags());
    }

    public TracingAsyncRestTemplateInterceptor(Tracer tracer, List<RestTemplateSpanDecorator> spanDecorators) {
        this.tracer = tracer;
        this.spanDecorators = new ArrayList<>(spanDecorators);
    }

    @Override
    public ListenableFuture<ClientHttpResponse> intercept(final HttpRequest httpRequest,
                                                          byte[] body,
                                                          AsyncClientHttpRequestExecution execution) throws IOException {

        Tracer.SpanBuilder spanBuilder = tracer.buildSpan(httpRequest.getMethod().toString())
                .withTag(Tags.SPAN_KIND.getKey(), Tags.SPAN_KIND_CLIENT);

        // link with parent span
        SpanManager.ManagedSpan parentSpan = spanManager.current();
        if (parentSpan.getSpan() != null) {
            spanBuilder.asChildOf(parentSpan.getSpan());
        }

        final Span span = spanBuilder.start();
        tracer.inject(span.context(), Format.Builtin.HTTP_HEADERS, new HttpHeadersCarrier(httpRequest.getHeaders()));

        for (RestTemplateSpanDecorator spanDecorator : spanDecorators) {
            try {
                spanDecorator.onRequest(httpRequest, span);
            } catch (RuntimeException exDecorator) {
                log.log(Level.SEVERE, "Exception during decorating span", exDecorator);
            }
        }

        ListenableFuture<ClientHttpResponse> future = execution.executeAsync(httpRequest, body);

        future.addCallback(new ListenableFutureCallback<ClientHttpResponse>() {
            @Override
            public void onSuccess(ClientHttpResponse httpResponse) {
                for (RestTemplateSpanDecorator spanDecorator: spanDecorators) {
                    try {
                        spanDecorator.onResponse(httpRequest, httpResponse, span);
                    } catch (RuntimeException exDecorator) {
                        log.log(Level.SEVERE, "Exception during decorating span", exDecorator);
                    }
                }
                span.finish();
            }

            @Override
            public void onFailure(Throwable ex) {
                for (RestTemplateSpanDecorator spanDecorator: spanDecorators) {
                    try {
                        spanDecorator.onError(httpRequest, ex, span);
                    } catch (RuntimeException exDecorator) {
                        log.log(Level.SEVERE, "Exception during decorating span", exDecorator);
                    }
                }
                span.finish();
            }
        });

        return future;
    }
}
