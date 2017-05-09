package io.opentracing.contrib.spring.web.client;

import io.opentracing.contrib.spanmanager.DefaultSpanManager;
import io.opentracing.mock.MockSpan;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.AsyncClientHttpRequestInterceptor;
import org.springframework.test.web.client.ExpectedCount;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.match.MockRestRequestMatchers;
import org.springframework.test.web.client.response.MockRestResponseCreators;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.web.client.AsyncRestTemplate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
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
    public void testMultipleRequests() throws InterruptedException {
        final MockSpan parentSpan = mockTracer.buildSpan("foo").start();

        final String url = "http://localhost:8080/foo";
        int numberOfCalls = 1000;
        mockServer.expect(ExpectedCount.manyTimes(), MockRestRequestMatchers.requestTo(url))
                .andRespond(MockRestResponseCreators.withSuccess());

        ExecutorService executorService = Executors.newFixedThreadPool(100);
        List<Future<?>> futures = new ArrayList<>(numberOfCalls);
        for (int i = 0; i < numberOfCalls; i++) {
            futures.add(executorService.submit(new Runnable() {
                @Override
                public void run() {
                    DefaultSpanManager.getInstance().activate(parentSpan);
                    client.getForEntity(url, String.class);
                }
            }));
        }

        // wait to finish all calls
        executorService.awaitTermination(1, TimeUnit.SECONDS);
        executorService.shutdown();

        parentSpan.finish();
        List<MockSpan> mockSpans = mockTracer.finishedSpans();
        Assert.assertEquals(numberOfCalls + 1, mockSpans.size());

        for (int i = 0; i < numberOfCalls; i++) {
            MockSpan mockSpan = mockSpans.get(0);

            Assert.assertEquals(parentSpan.context().traceId(), mockSpan.context().traceId());
            Assert.assertEquals(parentSpan.context().spanId(), mockSpan.parentId());
            Assert.assertEquals(0, mockSpan.generatedErrors().size());
        }
    }
}
