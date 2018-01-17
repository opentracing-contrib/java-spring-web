package io.opentracing.contrib.spring.web.client;

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

import org.junit.Assert;
import org.junit.Test;
import org.mockito.internal.matchers.Contains;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.AsyncClientHttpRequestInterceptor;
import org.springframework.test.web.client.ExpectedCount;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.match.MockRestRequestMatchers;
import org.springframework.test.web.client.response.MockRestResponseCreators;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.web.client.AsyncRestTemplate;

import io.opentracing.Scope;
import io.opentracing.Span;
import io.opentracing.mock.MockSpan;
import io.opentracing.tag.Tags;

/**
 * @author Pavol Loffay
 */
public class TracingAsyncRestTemplateTest extends AbstractTracingClientTest<AsyncRestTemplate> {

    public TracingAsyncRestTemplateTest() {
        final AsyncRestTemplate restTemplate = new AsyncRestTemplate();
        restTemplate.setInterceptors(Collections.<AsyncClientHttpRequestInterceptor>singletonList(
                new TracingAsyncRestTemplateInterceptor(mockTracer,
                        Collections.<RestTemplateSpanDecorator>singletonList(new RestTemplateSpanDecorator.StandardTags()))));

        client = new Client<AsyncRestTemplate>() {
            @Override
            public <T> ResponseEntity<T> getForEntity(String url, Class<T> clazz) {
                ListenableFuture<ResponseEntity<T>> forEntity = restTemplate.getForEntity(url, clazz);
                try {
                    return forEntity.get();
                } catch (InterruptedException | ExecutionException e) {
                    e.printStackTrace();
                    Assert.fail();
                }
                return null;
            }

            @Override
            public AsyncRestTemplate template() {
                return restTemplate;
            }
        };

        mockServer = MockRestServiceServer.bindTo(client.template()).ignoreExpectOrder(true).build();
    }

    @Test
    public void testMultipleRequests() throws InterruptedException, ExecutionException {
        final String url = "http://localhost:8080/foo/";
        int numberOfCalls = 1000;
        mockServer.expect(ExpectedCount.manyTimes(), MockRestRequestMatchers.requestTo(new Contains("/foo")))
                .andRespond(MockRestResponseCreators.withSuccess());

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
