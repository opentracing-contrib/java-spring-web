/*
 * Copyright 2016-2018 The OpenTracing Authors
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

import io.opentracing.Scope;
import io.opentracing.Span;
import io.opentracing.mock.MockSpan;
import io.opentracing.tag.Tags;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;

/**
 * @author Csaba Kos
 */
public class TracingWebClientTest extends AbstractTracingClientTest {

    public TracingWebClientTest() {
        super(tracer -> {
            final WebClient webClient = WebClient.builder()
                    .filter(new TracingExchangeFilterFunction(tracer,
                            Collections.singletonList(new WebClientSpanDecorator.StandardTags())))
                    .build();
            return new Client() {
                @Override
                public <T> ResponseEntity<T> getForEntity(String url, Class<T> clazz) {
                    Mono<ResponseEntity<T>> forEntity = webClient.get()
                            .uri(URI.create(url))
                            .exchange()
                            .flatMap(clientResponse -> clientResponse.toEntity(clazz));
                    return forEntity.block();
                }
            };
        }, WebClientSpanDecorator.StandardTags.COMPONENT_NAME);
    }

    @Test
    public void testMultipleRequests() throws InterruptedException, ExecutionException {
        final String url = wireMockRule.url("/foo/");
        int numberOfCalls = 1000;

        stubFor(get(urlPathMatching(".*/foo/.*"))
                .willReturn(aResponse().withStatus(200)));

        ExecutorService executorService = Executors.newFixedThreadPool(100);
        List<Future<?>> futures = new ArrayList<>(numberOfCalls);
        for (int i = 0; i < numberOfCalls; i++) {
            final String requestUrl = url + i;

            final Scope parentSpan = mockTracer.buildSpan("foo").startActive(false);
            parentSpan.span().setTag("request-url", requestUrl);

            final Span cont = parentSpan.span();

            futures.add(executorService.submit(new Runnable() {
                @Override
                public void run() {
                    try (Scope span = mockTracer.scopeManager().activate(cont, true)) {
                        client.getForEntity(requestUrl, String.class);
                    }
                }
            }));

            parentSpan.close();
        }

        // wait to finish all calls
        for (Future<?> future: futures) {
            future.get();
        }

        executorService.awaitTermination(1, TimeUnit.SECONDS);
        executorService.shutdown();

        List<MockSpan> mockSpans = mockTracer.finishedSpans();
        Assert.assertEquals(numberOfCalls * 2, mockSpans.size());

        final List<MockSpan> parentSpans = new ArrayList<>();
        final Map<Long, MockSpan> childSpans = new HashMap<>();

        for (MockSpan mockSpan: mockSpans) {
            if (mockSpan.tags().containsKey("request-url")) {
                parentSpans.add(mockSpan);
            } else {
                childSpans.put(mockSpan.parentId(), mockSpan);
            }

        }

        Assert.assertEquals(numberOfCalls, parentSpans.size());
        Assert.assertEquals(numberOfCalls, childSpans.size());

        for (MockSpan parentSpan: parentSpans) {
            MockSpan childSpan = childSpans.get(parentSpan.context().spanId());
            Assert.assertEquals(parentSpan.tags().get("request-url"), childSpan.tags().get(Tags.HTTP_URL.getKey()));

            Assert.assertEquals(parentSpan.context().traceId(), childSpan.context().traceId());
            Assert.assertEquals(parentSpan.context().spanId(), childSpan.parentId());
            Assert.assertEquals(0, childSpan.generatedErrors().size());
            Assert.assertEquals(0, parentSpan.generatedErrors().size());
        }
    }
}
