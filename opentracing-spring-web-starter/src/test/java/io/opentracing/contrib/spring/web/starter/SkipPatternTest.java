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

import io.opentracing.mock.MockSpan;
import io.opentracing.mock.MockTracer;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.concurrent.Callable;

import static org.awaitility.Awaitility.await;
import static org.junit.Assert.assertEquals;

/**
 * @author Pavol Loffay
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        classes = {SkipPatternTest.SpringConfiguration.class, SkipPatternTest.Controller.class},
        properties = {
                "opentracing.spring.web.skipPattern=/skip",
                "opentracing.spring.web.client.enabled=false"
        })
@RunWith(SpringJUnit4ClassRunner.class)
public class SkipPatternTest {

    @Configuration
    @EnableAutoConfiguration
    public static class SpringConfiguration {
        @Bean
        public MockTracer tracer() {
            return new MockTracer();
        }
    }

    @RestController
    public static class Controller {
        @RequestMapping("/skip")
        public String skip() {
            return "skip";
        }
        @RequestMapping("/hello")
        public String hello() {
            return "hello";
        }
    }

    @Autowired
    private TestRestTemplate testRestTemplate;
    @Autowired
    private MockTracer mockTracer;

    @Test
    public void testSkipPattern() {
        testRestTemplate.getForEntity("/skip", String.class);
        List<MockSpan> mockSpans = mockTracer.finishedSpans();
        assertEquals(0, mockSpans.size());
        testRestTemplate.getForEntity("/hello", String.class);
        await().until(new Callable<Boolean>() {
            @Override
            public Boolean call() {
                return mockTracer.finishedSpans().size() == 1;
            }
        });
        mockSpans = mockTracer.finishedSpans();
        assertEquals(1, mockSpans.size());
    }
}
