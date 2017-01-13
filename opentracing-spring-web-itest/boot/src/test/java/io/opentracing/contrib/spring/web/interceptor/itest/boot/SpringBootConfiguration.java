package io.opentracing.contrib.spring.web.interceptor.itest.boot;

import java.util.Arrays;
import java.util.List;

import javax.servlet.Filter;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

import io.opentracing.Tracer;
import io.opentracing.contrib.spring.web.interceptor.SpanDecorator;
import io.opentracing.contrib.spring.web.interceptor.TracingHandlerInterceptor;
import io.opentracing.contrib.spring.web.interceptor.itest.common.app.ExceptionFilter;
import io.opentracing.contrib.spring.web.interceptor.itest.common.app.TestController;
import io.opentracing.contrib.spring.web.interceptor.itest.common.app.TracingBeansConfiguration;
import io.opentracing.contrib.spring.web.interceptor.itest.common.app.WebSecurityConfig;
import io.opentracing.contrib.web.servlet.filter.TracingFilter;


/**
 * @author Pavol Loffay
 */
@Configuration
@EnableAsync
@EnableAutoConfiguration
@Import({TracingBeansConfiguration.class,
        WebSecurityConfig.class,
        TestController.class,
})
public class SpringBootConfiguration extends WebMvcConfigurerAdapter {

    @Autowired
    private List<SpanDecorator> spanDecorators;

    @Autowired
    private Tracer tracer;

    @Bean
    public Filter exceptionFilter() {
        return new ExceptionFilter();
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new TracingHandlerInterceptor(tracer, spanDecorators));
    }

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        registry.addViewController("/controllerView")
                .setStatusCode(HttpStatus.OK)
                .setViewName("staticView");
    }

    @Bean
    public FilterRegistrationBean tracingFilter() {
        TracingFilter tracingFilter = new TracingFilter(tracer,
                Arrays.asList(io.opentracing.contrib.web.servlet.filter.SpanDecorator.STANDARD_TAGS));

        FilterRegistrationBean filterRegistrationBean = new FilterRegistrationBean(tracingFilter);
        filterRegistrationBean.addUrlPatterns("/*");
        filterRegistrationBean.setOrder(Integer.MIN_VALUE);
        filterRegistrationBean.setAsyncSupported(true);

        return filterRegistrationBean;
    }
}

