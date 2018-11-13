package io.opentracing.contrib.spring.web.starter;

import io.opentracing.contrib.spring.web.starter.properties.WebTracingProperties;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.CollectionUtils;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

import io.opentracing.Tracer;
import io.opentracing.contrib.spring.tracer.configuration.TracerAutoConfiguration;
import io.opentracing.contrib.spring.web.interceptor.HandlerInterceptorSpanDecorator;
import io.opentracing.contrib.spring.web.interceptor.TracingHandlerInterceptor;
import io.opentracing.contrib.web.servlet.filter.ServletFilterSpanDecorator;
import io.opentracing.contrib.web.servlet.filter.TracingFilter;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import static java.lang.String.format;


/**
 * @author Pavol Loffay
 * @author Eddú Meléndez
 */
@Configuration
@ConditionalOnWebApplication
@ConditionalOnBean(Tracer.class)
@AutoConfigureAfter(TracerAutoConfiguration.class)
@EnableConfigurationProperties(WebTracingProperties.class)
@ConditionalOnClass(WebMvcConfigurerAdapter.class)
@ConditionalOnProperty(name = "opentracing.spring.web.enabled", havingValue = "true", matchIfMissing = true)
public class ServerTracingAutoConfiguration {
    private static final Log log = LogFactory.getLog(ServerTracingAutoConfiguration.class);

    private final ObjectProvider<List<ServletFilterSpanDecorator>> servletFilterSpanDecorator;
    private final ObjectProvider<List<HandlerInterceptorSpanDecorator>> interceptorSpanDecorator;

    public ServerTracingAutoConfiguration(ObjectProvider<List<ServletFilterSpanDecorator>> servletFilterSpanDecorator,
                                          ObjectProvider<List<HandlerInterceptorSpanDecorator>> interceptorSpanDecorator) {
        this.servletFilterSpanDecorator = servletFilterSpanDecorator;
        this.interceptorSpanDecorator = interceptorSpanDecorator;
    }

    @Bean
    @ConditionalOnMissingBean(TracingFilter.class)
    public FilterRegistrationBean tracingFilter(Tracer tracer, WebTracingProperties tracingConfiguration) {
        log.info(format("Creating %s bean with %s mapped to %s, skip pattern is \"%s\"",
                FilterRegistrationBean.class.getSimpleName(), TracingFilter.class.getSimpleName(),
                tracingConfiguration.getUrlPatterns().toString(),
                tracingConfiguration.getSkipPattern()));

        List<ServletFilterSpanDecorator> decorators = servletFilterSpanDecorator.getIfAvailable();
        if (CollectionUtils.isEmpty(decorators)) {
            decorators = Collections.singletonList(ServletFilterSpanDecorator.STANDARD_TAGS);
        }

        TracingFilter tracingFilter = new TracingFilter(tracer, decorators, tracingConfiguration.getSkipPattern());

        FilterRegistrationBean filterRegistrationBean = new FilterRegistrationBean(tracingFilter);
        filterRegistrationBean.setUrlPatterns(tracingConfiguration.getUrlPatterns());
        filterRegistrationBean.setOrder(tracingConfiguration.getOrder());
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
                List<HandlerInterceptorSpanDecorator> decorators = interceptorSpanDecorator.getIfAvailable();
                if (CollectionUtils.isEmpty(decorators)) {
                    decorators = Arrays.asList(HandlerInterceptorSpanDecorator.STANDARD_LOGS,
                            HandlerInterceptorSpanDecorator.HANDLER_METHOD_OPERATION_NAME);
                }

                registry.addInterceptor(new TracingHandlerInterceptor(tracer, decorators));

                super.addInterceptors(registry);
            }
        };
    }

}
