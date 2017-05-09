package io.opentracing.contrib.spring.web.client;

import io.opentracing.contrib.spanmanager.DefaultSpanManager;
import io.opentracing.mock.MockSpan;
import io.opentracing.tag.Tags;
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

import java.util.*;
import java.util.concurrent.*;

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

        mockServer = MockRestServiceServer.bindTo(client.template()).build();
    }

    @Test
    public void testMultipleRequests() throws InterruptedException, ExecutionException {
        final String url = "http://localhost:8080/foo/";
        int numberOfCalls = 1000;
        mockServer.expect(ExpectedCount.manyTimes(), MockRestRequestMatchers.requestTo(new Contains("/foo")))
                .andRespond(MockRestResponseCreators.withSuccess());

        Map<Long, MockSpan> parentSpans = new LinkedHashMap<>(numberOfCalls);

        ExecutorService executorService = Executors.newFixedThreadPool(100);
        List<Future<?>> futures = new ArrayList<>(numberOfCalls);
        for (int i = 0; i < numberOfCalls; i++) {
            final String requestUrl = url + i;

            final MockSpan parentSpan = mockTracer.buildSpan("foo").start();
            parentSpan.setTag("request-url", requestUrl);
            parentSpans.put(parentSpan.context().spanId(), parentSpan);

            futures.add(executorService.submit(new Runnable() {
                @Override
                public void run() {
                    DefaultSpanManager.getInstance().activate(parentSpan);
                    client.getForEntity(requestUrl, String.class);
                }
            }));
        }

        // wait to finish all calls
        for (Future<?> future: futures) {
            future.get();
        }

        executorService.awaitTermination(1, TimeUnit.SECONDS);
        executorService.shutdown();

        List<MockSpan> mockSpans = mockTracer.finishedSpans();
        Assert.assertEquals(numberOfCalls, mockSpans.size());

        for (int i = 0; i < numberOfCalls; i++) {
            MockSpan childSpan = mockSpans.get(i);
            MockSpan parentSpan = parentSpans.get(childSpan.parentId());

            Assert.assertEquals(parentSpan.tags().get("request-url"), childSpan.tags().get(Tags.HTTP_URL.getKey()));

            Assert.assertEquals(parentSpan.context().traceId(), childSpan.context().traceId());
            Assert.assertEquals(parentSpan.context().spanId(), childSpan.parentId());
            Assert.assertEquals(0, childSpan.generatedErrors().size());
            Assert.assertEquals(0, parentSpan.generatedErrors().size());
        }
    }
}
