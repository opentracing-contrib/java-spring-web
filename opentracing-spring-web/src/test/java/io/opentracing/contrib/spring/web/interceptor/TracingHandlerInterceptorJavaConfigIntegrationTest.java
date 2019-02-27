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
package io.opentracing.contrib.spring.web.interceptor;

import io.opentracing.Tracer;
import io.opentracing.mock.MockTracer;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import static org.junit.Assert.assertNotNull;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes = TracingHandlerInterceptorJavaConfigIntegrationTest.TestConfiguration.class)
public class TracingHandlerInterceptorJavaConfigIntegrationTest {

    @Autowired
    private TracingHandlerInterceptor interceptor;

    @Test
    public void testAutowired() {
        assertNotNull(interceptor);
    }


    @Configuration
    static class TestConfiguration {

        @Bean
        TracingHandlerInterceptor tracingHandlerInterceptor(final Tracer tracer) {
            return new TracingHandlerInterceptor(tracer);
        }

        @Bean
        Tracer tracer() {
            return new MockTracer();
        }
    }
}
