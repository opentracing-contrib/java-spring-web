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
package io.opentracing.contrib.spring.webflux.webfilter.itest.webflux;

import io.opentracing.contrib.spring.web.webfilter.TracingWebFilter;
import io.opentracing.contrib.spring.web.webfilter.WebFluxSpanDecorator;
import io.opentracing.mock.MockSpan;
import io.opentracing.mock.MockTracer;
import io.opentracing.tag.Tags;
import io.opentracing.util.ThreadLocalScopeManager;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.boot.web.embedded.jetty.JettyReactiveWebServerFactory;
import org.springframework.boot.web.reactive.context.ReactiveWebServerApplicationContext;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.HttpHandler;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebHandler;
import org.springframework.web.server.adapter.WebHttpHandlerBuilder;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

/**
 * @author Csaba Kos
 */
public class WebFluxJettyMinimalIntegrationTest {

    private static final ReactiveWebServerApplicationContext APPLICATION_CONTEXT = new ReactiveWebServerApplicationContext();
    private static final MockTracer MOCK_TRACER = Mockito.spy(new MockTracer(new ThreadLocalScopeManager(),
            MockTracer.Propagator.TEXT_MAP));
    private final static TracingWebFilter TRACING_WEB_FILTER = new TracingWebFilter(
            MOCK_TRACER,
            Integer.MIN_VALUE,
            Pattern.compile(""),
            Collections.singletonList("/*"),
            Arrays.asList(new WebFluxSpanDecorator.StandardTags(), new WebFluxSpanDecorator.WebFluxTags())
    );
    private static final int SERVER_PORT;
    private static final TestRestTemplate TEST_REST_TEMPLATE;

    static {
        APPLICATION_CONTEXT.registerBean("jettyReactiveWebServerFactory", JettyReactiveWebServerFactory.class, () ->
                new JettyReactiveWebServerFactory(0));
        APPLICATION_CONTEXT.registerBean("webHandler", WebHandler.class, () ->
                serverWebExchange -> TRACING_WEB_FILTER.filter(serverWebExchange, WebFluxJettyMinimalIntegrationTest::handler));
        APPLICATION_CONTEXT.registerBean("httpHandler", HttpHandler.class, () ->
                WebHttpHandlerBuilder.applicationContext(APPLICATION_CONTEXT).build());
        APPLICATION_CONTEXT.refresh();
        SERVER_PORT = APPLICATION_CONTEXT.getWebServer().getPort();
        TEST_REST_TEMPLATE = new TestRestTemplate(new RestTemplateBuilder()
                .rootUri("http://127.0.0.1:" + SERVER_PORT));
    }

    private static Mono<Void> handler(final ServerWebExchange serverWebExchange) {
        final ServerHttpResponse response = serverWebExchange.getResponse();
        response.setStatusCode(HttpStatus.OK);
        return response.writeWith(Mono.just(new DefaultDataBufferFactory().wrap("Hello World!\n".getBytes())));
    }

    @AfterClass
    public static void afterClass() {
        APPLICATION_CONTEXT.close();
    }

    @Before
    public void beforeTest() {
        MOCK_TRACER.reset();
        Mockito.reset(MOCK_TRACER);
    }

    @Test
    public void testGet() {
        TEST_REST_TEMPLATE.getForEntity("/", String.class);

        final List<MockSpan> mockSpans = MOCK_TRACER.finishedSpans();
        Assert.assertEquals(1, mockSpans.size());

        final MockSpan span = mockSpans.get(0);
        Assert.assertEquals("GET", span.operationName());

        Assert.assertEquals(8, span.tags().size());
        Assert.assertEquals(Tags.SPAN_KIND_SERVER, span.tags().get(Tags.SPAN_KIND.getKey()));
        Assert.assertEquals("GET", span.tags().get(Tags.HTTP_METHOD.getKey()));
        Assert.assertEquals(TEST_REST_TEMPLATE.getRootUri() + "/", span.tags().get(Tags.HTTP_URL.getKey()));
        Assert.assertEquals(200, span.tags().get(Tags.HTTP_STATUS.getKey()));
        Assert.assertEquals("WebFlux/java-spring-web", span.tags().get(Tags.COMPONENT.getKey()));
        Assert.assertNotNull(span.tags().get(Tags.PEER_PORT.getKey()));
        Assert.assertEquals("127.0.0.1", span.tags().get(Tags.PEER_HOST_IPV4.getKey()));
    }
}
