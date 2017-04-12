package io.opentracing.contrib.spring.web.autoconfig;

import java.util.Collections;
import java.util.logging.Logger;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

import io.opentracing.Tracer;
import io.opentracing.contrib.spring.web.interceptor.HandlerInterceptorSpanDecorator;
import io.opentracing.contrib.spring.web.interceptor.TracingHandlerInterceptor;
import io.opentracing.contrib.web.servlet.filter.ServletFilterSpanDecorator;
import io.opentracing.contrib.web.servlet.filter.TracingFilter;

/**
 * @author Pavol Loffay
 */
@Configuration
@ConditionalOnMissingBean(TracingFilter.class)
public class ServerTracingAutoConfiguration {
    private static final Logger log = Logger.getLogger(ServerTracingAutoConfiguration.class.getName());

    @Bean
    @ConditionalOnMissingBean(WebTracingConfiguration.class)
    public WebTracingConfiguration tracerAutoConfiguration() {
        return WebTracingConfiguration.builder()
                .withSkipPattern(WebTracingConfiguration.DEFAULT_SKIP_PATTERN).build();
    }

    @Bean
    public FilterRegistrationBean tracingFilter(Tracer tracer, WebTracingConfiguration tracingConfiguration) {
        log.info("Creating " + FilterRegistrationBean.class.getSimpleName() + " bean with " +
                TracingFilter.class + " mapped to " + "/*, skip pattern is " + tracingConfiguration.getSkipPattern());

        TracingFilter tracingFilter = new TracingFilter(tracer,
                Collections.singletonList(ServletFilterSpanDecorator.STANDARD_TAGS), tracingConfiguration.getSkipPattern());

        FilterRegistrationBean filterRegistrationBean = new FilterRegistrationBean(tracingFilter);
        filterRegistrationBean.addUrlPatterns("/*");
        filterRegistrationBean.setOrder(Integer.MIN_VALUE);
        filterRegistrationBean.setAsyncSupported(true);

        return filterRegistrationBean;
    }

    @Bean
    public WebMvcConfigurerAdapter tracingHandlerInterceptor(final Tracer tracer) {
        log.info("Creating " + WebMvcConfigurerAdapter.class.getSimpleName() + " bean with " +
                TracingHandlerInterceptor.class);

        return new WebMvcConfigurerAdapter() {
            @Override
            public void addInterceptors(InterceptorRegistry registry) {
                registry.addInterceptor(new TracingHandlerInterceptor(tracer,
                        Collections.singletonList(HandlerInterceptorSpanDecorator.STANDARD_TAGS)));
                super.addInterceptors(registry);
            }
        };
    }
}
