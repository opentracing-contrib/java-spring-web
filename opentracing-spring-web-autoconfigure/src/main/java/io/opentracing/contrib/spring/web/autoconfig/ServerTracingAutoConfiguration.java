package io.opentracing.contrib.spring.web.autoconfig;

import java.util.Arrays;
import java.util.Collections;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
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
 * @author Eddú Meléndez
 */
@Configuration
@ConditionalOnWebApplication
@ConditionalOnBean(Tracer.class)
@AutoConfigureAfter(TracerAutoConfiguration.class)
@EnableConfigurationProperties(WebTracingProperties.class)
public class ServerTracingAutoConfiguration {
    private static final Log log = LogFactory.getLog(ServerTracingAutoConfiguration.class);

    @Bean
    @ConditionalOnMissingBean(TracingFilter.class)
    public FilterRegistrationBean tracingFilter(Tracer tracer, WebTracingProperties tracingConfiguration) {
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
    @ConditionalOnMissingBean(TracingFilter.class)
    public WebMvcConfigurerAdapter tracingHandlerInterceptor(final Tracer tracer) {
        log.info("Creating " + WebMvcConfigurerAdapter.class.getSimpleName() + " bean with " +
                TracingHandlerInterceptor.class);

        return new WebMvcConfigurerAdapter() {
            @Override
            public void addInterceptors(InterceptorRegistry registry) {
                registry.addInterceptor(new TracingHandlerInterceptor(tracer,
                        Arrays.asList(HandlerInterceptorSpanDecorator.STANDARD_LOGS,
                                HandlerInterceptorSpanDecorator.HANDLER_METHOD_OPERATION_NAME)));
                super.addInterceptors(registry);
            }
        };
    }
}
