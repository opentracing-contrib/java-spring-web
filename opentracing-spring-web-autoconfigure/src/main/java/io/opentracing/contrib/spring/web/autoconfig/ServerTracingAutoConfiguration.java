package io.opentracing.contrib.spring.web.autoconfig;

import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
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

    @Autowired(required=false)
    @Qualifier("opentracing.http.skipPattern")
    private Set<String> skipPatterns;

    @Bean
    @ConditionalOnMissingBean(WebTracingConfiguration.class)
    public WebTracingConfiguration tracerAutoConfiguration() {
        Pattern skipPattern = WebTracingConfiguration.DEFAULT_SKIP_PATTERN;
        if (skipPatterns != null && !skipPatterns.isEmpty()) {
            StringBuilder pattern = new StringBuilder();
            for (String p : skipPatterns) {
                if (pattern.length() > 0) {
                    pattern.append('|');
                }
                pattern.append(p);
            }
            skipPattern = Pattern.compile(pattern.toString());
        }
        return WebTracingConfiguration.builder()
                .withSkipPattern(skipPattern).build();
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
                        Arrays.asList(HandlerInterceptorSpanDecorator.STANDARD_LOGS,
                                HandlerInterceptorSpanDecorator.HANDLER_METHOD_OPERATION_NAME)));
                super.addInterceptors(registry);
            }
        };
    }
}
