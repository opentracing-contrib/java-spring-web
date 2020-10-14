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
package io.opentracing.contrib.spring.web.starter;

import io.opentracing.contrib.spring.web.client.TracingRestTemplateInterceptor;
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
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.AsyncClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.web.client.AsyncRestTemplate;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        classes = {RestTemplateTracingAutoConfigurationDisabledWebTest.SpringConfiguration.class},
        properties = {"opentracing.spring.web.enabled=false"})
@RunWith(SpringJUnit4ClassRunner.class)
@ActiveProfiles("test")
public class RestTemplateTracingAutoConfigurationDisabledWebTest extends AutoConfigurationBaseTest {

    @Configuration
    @EnableAutoConfiguration
    public static class SpringConfiguration {

        @Bean
        public MockTracer tracer() {
            return new MockTracer(new ThreadLocalScopeManager());
        }

        @Bean
        public RestTemplate restTemplate() {
            return new RestTemplate();
        }

        @Bean
        public AsyncRestTemplate asyncRestTemplate() {
            return new AsyncRestTemplate();
        }
    }

    @Autowired
    private MockTracer mockTracer;
    @Autowired
    private RestTemplate restTemplate;
    @Autowired
    private AsyncRestTemplate asyncRestTemplate;

    @Before
    public void setUp() {
        mockTracer.reset();
    }

    @Test
    public void testInterceptorNotRegistered() {
        for (ClientHttpRequestInterceptor interceptor : restTemplate.getInterceptors()) {
            assertThat(interceptor).isNotInstanceOf(TracingRestTemplateInterceptor.class);
        }
    }

    @Test
    public void testAsyncInterceptorNotRegistered() {
        for (AsyncClientHttpRequestInterceptor interceptor : asyncRestTemplate.getInterceptors()) {
            assertThat(interceptor).isNotInstanceOf(TracingRestTemplateInterceptor.class);
        }
    }

    @Test
    public void testRestClientNotTracing() {
        try {
            restTemplate.getForEntity("http://nonexisting.example.com", String.class);
        } catch (ResourceAccessException ex) {
            //ok UnknownHostException
        }
        Assert.assertEquals(0, mockTracer.finishedSpans().size());
    }

    @Test
    public void testAsyncRestClientNotTracing() {
        ListenableFuture<ResponseEntity<String>> future = asyncRestTemplate.getForEntity("http://nonexisting.example.com", String.class);

        AtomicBoolean done = AsyncRestTemplatePostProcessingConfigurationTest.addDoneCallback(future);
        Awaitility.await().atMost(500, TimeUnit.MILLISECONDS).untilAtomic(done, IsEqual.equalTo(true));

        Assert.assertEquals(0, mockTracer.finishedSpans().size());
    }
}
