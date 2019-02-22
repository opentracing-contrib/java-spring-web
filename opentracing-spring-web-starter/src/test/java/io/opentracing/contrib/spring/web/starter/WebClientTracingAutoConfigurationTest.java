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
package io.opentracing.contrib.spring.web.starter;

import io.opentracing.mock.MockTracer;
import io.opentracing.util.ThreadLocalScopeManager;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.web.reactive.function.client.WebClient;

import java.net.URI;

/**
 * @author Csaba Kos
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        classes = {WebClientTracingAutoConfigurationTest.SpringConfiguration.class})
@RunWith(SpringJUnit4ClassRunner.class)
public class WebClientTracingAutoConfigurationTest extends AutoConfigurationBaseTest {

    @Configuration
    @EnableAutoConfiguration
    public static class SpringConfiguration {
        @Bean
        public MockTracer tracer() {
            return new MockTracer(new ThreadLocalScopeManager());
        }

        @Bean
        public WebClient webClient() {
            return WebClient.create();
        }

        @Bean
        public WebClient.Builder webClientBuilder() {
            return WebClient.builder();
        }
    }

    @Autowired
    private MockTracer mockTracer;

    @Autowired
    private WebClient webClient;

    @Autowired
    private WebClient.Builder webClientBuilder;

    @Before
    public void setUp() {
        mockTracer.reset();
    }

    @Test
    public void testWebClientAutoConfiguration() {
        webClient.get()
                .uri(URI.create("http://example.com"))
                .retrieve()
                .bodyToMono(String.class)
                .block();

        Assert.assertEquals(1, mockTracer.finishedSpans().size());
    }

    @Test
    public void testWebClientBuilderAutoConfiguration() {
        webClientBuilder
                .build()
                .get()
                .uri(URI.create("http://example.com"))
                .retrieve()
                .bodyToMono(String.class)
                .block();

        Assert.assertEquals(1, mockTracer.finishedSpans().size());
    }
}
