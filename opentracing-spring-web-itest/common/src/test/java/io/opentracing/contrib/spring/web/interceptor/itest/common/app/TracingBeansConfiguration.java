package io.opentracing.contrib.spring.web.interceptor.itest.common.app;

import java.util.Arrays;
import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.opentracing.contrib.spring.web.interceptor.SpanDecorator;
import io.opentracing.mock.MockTracer;

/**
 * @author Pavol Loffay
 */
@Configuration
public class TracingBeansConfiguration {

    public static final MockTracer mockTracer = new MockTracer(MockTracer.Propagator.TEXT_MAP);

    @Bean
    public MockTracer mockTracerBean() {
        return mockTracer;
    }

    @Bean
    public List<SpanDecorator> spanDecorators() {
        return Arrays.asList(SpanDecorator.STANDARD_TAGS);
    }
}
