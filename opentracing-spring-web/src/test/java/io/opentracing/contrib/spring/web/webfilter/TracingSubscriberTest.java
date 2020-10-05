/**
 * Copyright 2016-2020 The OpenTracing Authors
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
package io.opentracing.contrib.spring.web.webfilter;

import io.opentracing.Scope;
import io.opentracing.Span;
import io.opentracing.mock.MockTracer;
import io.opentracing.util.ThreadLocalScopeManager;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.http.HttpMethod;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.Disposable;
import reactor.core.publisher.Mono;
import reactor.core.publisher.SignalType;
import reactor.core.scheduler.Schedulers;

import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class TracingSubscriberTest {

    private final MockTracer tracer = new MockTracer(new ThreadLocalScopeManager());

    @Mock
    private ServerWebExchange exchange;

    @Mock
    private ServerHttpRequest request;

    @Mock
    private WebFluxSpanDecorator spanDecorator;

    @Before
    public void resetTracer() {
        tracer.reset();
    }

    @Before
    public void mockExchange() {
        given(exchange.getRequest()).willReturn(request);
        given(request.getMethodValue()).willReturn(HttpMethod.GET.name());
    }

    @Test
    public void testSpanIsFinishedWhenMonoHasCompleted() throws InterruptedException {
        Span span = tracer.buildSpan("a span").start();

        AtomicReference<SignalType> finalSignalType = new AtomicReference<>();
        CountDownLatch finalSignalCountDownLatch = new CountDownLatch(1);

        Mono<Void> source = Mono.just("5")
            .then()
            .doFinally(signalType -> {
                finalSignalType.set(signalType);
                finalSignalCountDownLatch.countDown();
            })
            .subscribeOn(Schedulers.single());

        try (Scope scope = tracer.activateSpan(span)) {
            new TracingOperator(source, exchange, tracer, Arrays.asList(spanDecorator))
                .subscribe();
            finalSignalCountDownLatch.await();
        }

        assertEquals(1, tracer.finishedSpans().size());
        assertEquals(SignalType.ON_COMPLETE, finalSignalType.get());

        Span finishedSpan = tracer.finishedSpans().get(0);
        verify(spanDecorator).onRequest(exchange, finishedSpan);
        verify(spanDecorator).onResponse(exchange, finishedSpan);
        verify(spanDecorator, never()).onError(any(ServerWebExchange.class), any(Throwable.class), any(Span.class));
    }

    @Test
    public void testSpanIsFinishedWhenMonoHasError() throws InterruptedException {
        Span span = tracer.buildSpan("a span").start();

        AtomicReference<SignalType> finalSignalType = new AtomicReference<>();
        CountDownLatch finalSignalCountDownLatch = new CountDownLatch(2);

        Mono<Void> source = Mono.error(new Exception("An error"))
            .then()
            .doOnError(error -> finalSignalCountDownLatch.countDown())
            .doFinally(signalType -> {
                finalSignalType.set(signalType);
                finalSignalCountDownLatch.countDown();
            })
            .subscribeOn(Schedulers.single());

        try (Scope scope = tracer.activateSpan(span)) {
            new TracingOperator(source, exchange, tracer, Arrays.asList(spanDecorator))
                .subscribe();
            finalSignalCountDownLatch.await();
        }

        assertEquals(1, tracer.finishedSpans().size());
        assertEquals(SignalType.ON_ERROR, finalSignalType.get());

        Span finishedSpan = tracer.finishedSpans().get(0);
        verify(spanDecorator).onRequest(exchange, finishedSpan);
        verify(spanDecorator).onError(eq(exchange), any(Throwable.class), eq(finishedSpan));
        verify(spanDecorator, never()).onResponse(any(ServerWebExchange.class), any(Span.class));
    }

    @Test
    public void testSpanIsFinishedWhenMonoHasCanceled() throws InterruptedException {
        Span span = tracer.buildSpan("a span").start();

        AtomicReference<SignalType> finalSignalType = new AtomicReference<>();
        CountDownLatch finalSignalCountDownLatch = new CountDownLatch(1);

        CountDownLatch canBeDisposedCountDownLatch = new CountDownLatch(1);
        CountDownLatch disposedCountDownLatch = new CountDownLatch(1);

        Mono<Void> source = Mono.just("5")
            .doOnNext(str -> {
                try {
                    canBeDisposedCountDownLatch.countDown();
                    disposedCountDownLatch.await();
                } catch (InterruptedException ignored) {
                }
            })
            .then()
            .doFinally(signalType -> {
                finalSignalType.set(signalType);
                finalSignalCountDownLatch.countDown();
            })
            .subscribeOn(Schedulers.single());


        try (Scope scope = tracer.activateSpan(span)) {
            Disposable disposable = new TracingOperator(source, exchange, tracer, Arrays.asList(spanDecorator))
                .subscribe();

            canBeDisposedCountDownLatch.await();
            disposable.dispose();
            disposedCountDownLatch.countDown();
            finalSignalCountDownLatch.await();
        }


        assertEquals(1, tracer.finishedSpans().size());
        assertEquals(SignalType.CANCEL, finalSignalType.get());

        Span finishedSpan = tracer.finishedSpans().get(0);
        verify(spanDecorator).onRequest(exchange, finishedSpan);
        verify(spanDecorator, never()).onError(any(ServerWebExchange.class), any(Throwable.class), any(Span.class));
        verify(spanDecorator, never()).onResponse(any(ServerWebExchange.class), any(Span.class));
    }
}
