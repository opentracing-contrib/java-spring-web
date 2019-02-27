/**
 * Copyright 2016-2019 The OpenTracing Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package io.opentracing.contrib.spring.web.client;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.AsyncClientHttpRequestExecution;
import org.springframework.http.client.AsyncClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.concurrent.ListenableFutureCallback;

import io.opentracing.Scope;
import io.opentracing.Span;
import io.opentracing.Tracer;
import io.opentracing.propagation.Format;
import io.opentracing.tag.Tags;
import io.opentracing.util.GlobalTracer;

/**
 * Note: From Spring Framework 5, {@link org.springframework.web.client.AsyncRestTemplate} is deprecated.
 *
 * @author Pavol Loffay
 */
public class TracingAsyncRestTemplateInterceptor implements AsyncClientHttpRequestInterceptor {
    private static final Log log = LogFactory.getLog(TracingAsyncRestTemplateInterceptor.class);

    private Tracer tracer;
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

        final Scope scope = tracer.buildSpan(httpRequest.getMethod().toString())
                .withTag(Tags.SPAN_KIND.getKey(), Tags.SPAN_KIND_CLIENT).startActive(false);
        tracer.inject(scope.span().context(), Format.Builtin.HTTP_HEADERS, new HttpHeadersCarrier(httpRequest.getHeaders()));

        for (RestTemplateSpanDecorator spanDecorator : spanDecorators) {
            try {
                spanDecorator.onRequest(httpRequest, scope.span());
            } catch (RuntimeException exDecorator) {
                log.error("Exception during decorating span", exDecorator);
            }
        }

        ListenableFuture<ClientHttpResponse> future = execution.executeAsync(httpRequest, body);

        final Span span = scope.span();

        future.addCallback(new ListenableFutureCallback<ClientHttpResponse>() {
            @Override
            public void onSuccess(ClientHttpResponse httpResponse) {
                try (Scope asyncScope = tracer.scopeManager().activate(span, true)) {
                    for (RestTemplateSpanDecorator spanDecorator: spanDecorators) {
                        try {
                            spanDecorator.onResponse(httpRequest, httpResponse, scope.span());
                        } catch (RuntimeException exDecorator) {
                            log.error("Exception during decorating span", exDecorator);
                        }
                    }
                }
            }

            @Override
            public void onFailure(Throwable ex) {
                try (Scope asyncScope = tracer.scopeManager().activate(span, true)) {
                    for (RestTemplateSpanDecorator spanDecorator: spanDecorators) {
                        try {
                            spanDecorator.onError(httpRequest, ex, scope.span());
                        } catch (RuntimeException exDecorator) {
                            log.error("Exception during decorating span", exDecorator);
                        }
                    }
                }
            }
        });

        scope.close();

        return future;
    }
}
