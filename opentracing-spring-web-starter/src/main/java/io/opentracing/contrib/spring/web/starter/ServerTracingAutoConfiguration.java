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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
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
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

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
 * @author Gilles Robert
 */
@Configuration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnBean(Tracer.class)
@AutoConfigureAfter({TracerAutoConfiguration.class, SkipPatternAutoConfiguration.class})
@EnableConfigurationProperties(WebTracingProperties.class)
@ConditionalOnClass(WebMvcConfigurer.class)
@ConditionalOnProperty(name = "opentracing.spring.web.enabled", havingValue = "true", matchIfMissing = true)
public class ServerTracingAutoConfiguration {

    private static final Log log = LogFactory.getLog(ServerTracingAutoConfiguration.class);

    private final Pattern skipPattern;
    private final ObjectProvider<List<ServletFilterSpanDecorator>> servletFilterSpanDecorator;
    private final ObjectProvider<List<HandlerInterceptorSpanDecorator>> interceptorSpanDecorator;

    public ServerTracingAutoConfiguration(@Qualifier("skipPattern") Pattern skipPattern,
                                          ObjectProvider<List<ServletFilterSpanDecorator>> servletFilterSpanDecorator,
                                          ObjectProvider<List<HandlerInterceptorSpanDecorator>> interceptorSpanDecorator) {
        this.skipPattern = skipPattern;
        this.servletFilterSpanDecorator = servletFilterSpanDecorator;
        this.interceptorSpanDecorator = interceptorSpanDecorator;
    }

    @Bean
    @ConditionalOnMissingBean(TracingFilter.class)
    public FilterRegistrationBean tracingFilter(Tracer tracer, WebTracingProperties tracingConfiguration) {
        log.info(format("Creating %s bean with %s mapped to %s, skip pattern is \"%s\"",
                FilterRegistrationBean.class.getSimpleName(), TracingFilter.class.getSimpleName(),
                tracingConfiguration.getUrlPatterns().toString(), skipPattern));

        List<ServletFilterSpanDecorator> decorators = servletFilterSpanDecorator.getIfAvailable();
        if (CollectionUtils.isEmpty(decorators)) {
            decorators = Collections.singletonList(ServletFilterSpanDecorator.STANDARD_TAGS);
        }

        TracingFilter tracingFilter = new TracingFilter(tracer, decorators, skipPattern);

        FilterRegistrationBean filterRegistrationBean = new FilterRegistrationBean<>(tracingFilter);
        filterRegistrationBean.setUrlPatterns(tracingConfiguration.getUrlPatterns());
        filterRegistrationBean.setOrder(tracingConfiguration.getOrder());
        filterRegistrationBean.setAsyncSupported(true);

        return filterRegistrationBean;
    }

    @Bean
    @ConditionalOnMissingBean(TracingFilter.class)
    public WebMvcConfigurer tracingHandlerInterceptor(final Tracer tracer) {
        log.info("Creating " + WebMvcConfigurer.class.getSimpleName() + " bean with " +
                TracingHandlerInterceptor.class);

        return new WebMvcConfigurer() {
            @Override
            public void addInterceptors(InterceptorRegistry registry) {
                List<HandlerInterceptorSpanDecorator> decorators = interceptorSpanDecorator.getIfAvailable();
                if (CollectionUtils.isEmpty(decorators)) {
                    decorators = Arrays.asList(HandlerInterceptorSpanDecorator.STANDARD_LOGS,
                            HandlerInterceptorSpanDecorator.HANDLER_METHOD_OPERATION_NAME);
                }

                registry.addInterceptor(new TracingHandlerInterceptor(tracer, decorators));
            }
        };
    }

}
