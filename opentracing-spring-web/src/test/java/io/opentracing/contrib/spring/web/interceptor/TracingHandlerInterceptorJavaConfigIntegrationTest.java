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
