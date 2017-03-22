package io.opentracing.contrib.spring.web;

import java.util.Arrays;
import java.util.logging.Logger;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

import io.opentracing.NoopTracer;
import io.opentracing.NoopTracerFactory;
import io.opentracing.Tracer;
import io.opentracing.contrib.spring.web.interceptor.SpanDecorator;
import io.opentracing.contrib.spring.web.interceptor.TracingHandlerInterceptor;
import io.opentracing.contrib.web.servlet.filter.TracingFilter;

/**
 * @author Pavol Loffay
 */
@Configuration
@ConditionalOnMissingBean(TracingFilter.class)
public class ServerTracingAutoConfiguration {
    private static final Logger log = Logger.getLogger(ServerTracingAutoConfiguration.class.getName());

    @Autowired
    private Tracer tracer;

    @Bean
    @ConditionalOnMissingBean(Tracer.class)
    public Tracer noopTracer() {
        log.info("Tracer bean is not configured! Switching to " + NoopTracer.class.getName());
        return NoopTracerFactory.create();
    }

    @Bean
    public FilterRegistrationBean tracingFilter() {
        log.info("Creating " + FilterRegistrationBean.class.getSimpleName() + " bean with " +
                TracingFilter.class + " mapped to " + "/*");

        TracingFilter tracingFilter = new TracingFilter(tracer,
                Arrays.asList(io.opentracing.contrib.web.servlet.filter.SpanDecorator.STANDARD_TAGS));

        FilterRegistrationBean filterRegistrationBean = new FilterRegistrationBean(tracingFilter);
        filterRegistrationBean.addUrlPatterns("/*");
        filterRegistrationBean.setOrder(Integer.MIN_VALUE);
        filterRegistrationBean.setAsyncSupported(true);

        return filterRegistrationBean;
    }

    @Bean
    public WebMvcConfigurerAdapter tracingHandlerInterceptor() {
        log.info("Creating " + WebMvcConfigurerAdapter.class.getSimpleName() + " bean with " +
                TracingHandlerInterceptor.class);

        return new WebMvcConfigurerAdapter() {
            @Override
            public void addInterceptors(InterceptorRegistry registry) {
                registry.addInterceptor(new TracingHandlerInterceptor(tracer,
                        Arrays.asList(SpanDecorator.STANDARD_TAGS)));
                super.addInterceptors(registry);
            }
        };
    }
}
