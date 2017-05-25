package io.opentracing.contrib.spring.web.interceptor.itest.common.app;

import java.util.Arrays;
import java.util.List;

import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.opentracing.contrib.spring.web.interceptor.HandlerInterceptorSpanDecorator;
import io.opentracing.mock.MockTracer;
import io.opentracing.util.ThreadLocalActiveSpanSource;

/**
 * @author Pavol Loffay
 */
@Configuration
public class TracingBeansConfiguration {

    public static final MockTracer mockTracer = Mockito.spy(new MockTracer(new ThreadLocalActiveSpanSource(),
            MockTracer.Propagator.TEXT_MAP));

    @Bean
    public MockTracer mockTracerBean() {
        return mockTracer;
    }

    @Bean
    public List<HandlerInterceptorSpanDecorator> spanDecorators() {
        return Arrays.asList(HandlerInterceptorSpanDecorator.STANDARD_TAGS);
    }
}
