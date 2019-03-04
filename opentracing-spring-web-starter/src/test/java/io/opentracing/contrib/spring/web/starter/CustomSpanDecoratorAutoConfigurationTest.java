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
package io.opentracing.contrib.spring.web.starter;

import io.opentracing.Span;
import io.opentracing.contrib.spring.web.client.RestTemplateSpanDecorator;
import io.opentracing.contrib.spring.web.client.WebClientSpanDecorator;
import io.opentracing.mock.MockTracer;
import io.opentracing.util.ThreadLocalScopeManager;
import org.awaitility.Awaitility;
import org.hamcrest.core.IsEqual;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.NestedExceptionUtils;
import org.springframework.http.HttpRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.web.client.AsyncRestTemplate;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;

import java.net.URI;
import java.net.UnknownHostException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author Michal Dvorak
 * @since 4/5/18
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        classes = {
                CustomSpanDecoratorAutoConfigurationTest.SpringConfiguration.class,
                CustomSpanDecoratorAutoConfigurationTest.ClientConfiguration.class,
        })
@RunWith(SpringJUnit4ClassRunner.class)
@ActiveProfiles("test")
public class CustomSpanDecoratorAutoConfigurationTest extends AutoConfigurationBaseTest {

    @Configuration
    @EnableAutoConfiguration
    public static class SpringConfiguration {

        @Bean
        public MockTracer tracer() {
            return new MockTracer(new ThreadLocalScopeManager());
        }

        @Bean
        public RestTemplateSpanDecorator customSpanDecorator() {
            return new RestTemplateSpanDecorator() {
                @Override
                public void onRequest(HttpRequest request, Span span) {
                    span.setTag("custom-test", "foo");
                }

                @Override
                public void onResponse(HttpRequest request, ClientHttpResponse response, Span span) {
                }

                @Override
                public void onError(HttpRequest request, Throwable ex, Span span) {
                }
            };
        }

        @Bean
        public WebClientSpanDecorator customWebClientSpanDecorator() {
            return new WebClientSpanDecorator() {
                @Override
                public void onRequest(final ClientRequest clientRequest, final Span span) {
                    span.setTag("custom-test", "foo");
                }

                @Override
                public void onResponse(final ClientRequest clientRequest, final ClientResponse clientResponse, final Span span) {

                }

                @Override
                public void onError(final ClientRequest clientRequest, final Throwable throwable, final Span span) {

                }

                @Override
                public void onCancel(final ClientRequest clientRequest, final Span span) {

                }
            };
        }
    }

    @Configuration
    public static class ClientConfiguration {

        @Autowired
        private RestTemplateBuilder builder;

        @Bean
        public RestTemplate restTemplate() {
            return builder.build();
        }

        @Bean
        public AsyncRestTemplate asyncRestTemplate() {
            return new AsyncRestTemplate();
        }

        @Bean
        public WebClient webClient(final WebClient.Builder builder) {
            return builder.build();
        }
    }

    @Autowired
    private MockTracer mockTracer;
    @Autowired
    private RestTemplate restTemplate;
    @Autowired
    private AsyncRestTemplate asyncRestTemplate;
    @Autowired
    private WebClient webClient;

    @Before
    public void setUp() {
        mockTracer.reset();
    }

    @Test
    public void testRestClientCustomTracing() {
        try {
            restTemplate.getForEntity("http://nonexisting.example.com", String.class);
        } catch (ResourceAccessException ex) {
            //ok UnknownHostException
        }
        Assert.assertEquals(1, mockTracer.finishedSpans().size());
        Assert.assertEquals("foo", mockTracer.finishedSpans().get(0).tags().get("custom-test"));
    }

    @Test
    public void testAsyncRestClientCustomTracing() {
        ListenableFuture<ResponseEntity<String>> future = asyncRestTemplate.getForEntity("http://nonexisting.example.com", String.class);

        AtomicBoolean done = AsyncRestTemplatePostProcessingConfigurationTest.addDoneCallback(future);
        Awaitility.await().atMost(1000, TimeUnit.MILLISECONDS).untilAtomic(done, IsEqual.equalTo(true));

        Assert.assertEquals(1, mockTracer.finishedSpans().size());
        Assert.assertEquals("foo", mockTracer.finishedSpans().get(0).tags().get("custom-test"));
    }

    @Test
    public void testWebClientCustomTracing() {
        try {
            webClient.get()
                    .uri(URI.create("http://nonexisting.example.com"))
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
        } catch (final RuntimeException e) {
            Assert.assertTrue(NestedExceptionUtils.getRootCause(e) instanceof UnknownHostException);
        }

        Assert.assertEquals(1, mockTracer.finishedSpans().size());
        Assert.assertEquals("foo", mockTracer.finishedSpans().get(0).tags().get("custom-test"));
    }
}
