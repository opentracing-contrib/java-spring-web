package io.opentracing.contrib.spring.web.interceptor.itest.mvc;

import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.config.annotation.DefaultServletHandlerConfigurer;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;
import org.springframework.web.servlet.view.InternalResourceViewResolver;

import io.opentracing.Tracer;
import io.opentracing.contrib.spring.web.client.TracingRestTemplateInterceptor;
import io.opentracing.contrib.spring.web.interceptor.HandlerInterceptorSpanDecorator;
import io.opentracing.contrib.spring.web.interceptor.TracingHandlerInterceptor;
import io.opentracing.contrib.spring.web.interceptor.itest.common.app.TestController;
import io.opentracing.contrib.spring.web.interceptor.itest.common.app.TestInterceptor;
import io.opentracing.contrib.spring.web.interceptor.itest.common.app.TracingBeansConfiguration;
import io.opentracing.contrib.spring.web.interceptor.itest.common.app.WebSecurityConfig;
import io.opentracing.contrib.web.servlet.filter.ServletFilterSpanDecorator;
import io.opentracing.contrib.web.servlet.filter.TracingFilter;
import io.opentracing.util.GlobalTracer;

/**
 * @author Pavol Loffay
 */
@EnableWebMvc
@Configuration
@Import({WebSecurityConfig.class,
        TracingBeansConfiguration.class,
        TestController.class})
public class SpringMVCConfiguration extends WebMvcConfigurerAdapter implements ServletContextListener {

    @Autowired
    private List<HandlerInterceptorSpanDecorator> spanDecorators;

    @Autowired
    private Tracer tracer;


    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        GlobalTracer.register(tracer);
        registry.addInterceptor(new TracingHandlerInterceptor(tracer, spanDecorators));
        registry.addInterceptor(new TestInterceptor());
    }

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        registry.addViewController("/controllerView")
                .setStatusCode(HttpStatus.OK)
                .setViewName("staticView");
    }

    @Override
    public void configureDefaultServletHandling(DefaultServletHandlerConfigurer configurer) {
        configurer.enable();
    }

    /**
     * Basic setup for JSP views.
     */
    @Bean
    public InternalResourceViewResolver configureInternalResourceViewResolver() {
        InternalResourceViewResolver resolver =
                new InternalResourceViewResolver();
        resolver.setPrefix("/WEB-INF/jsp/");
        resolver.setSuffix(".jsp");
        return resolver;
    }

    @Bean
    public RestTemplate restTemplate(Tracer tracer) {
        RestTemplate restTemplate = new RestTemplate();
        restTemplate.setInterceptors(Collections.<ClientHttpRequestInterceptor>singletonList(
                new TracingRestTemplateInterceptor(tracer)));
        return restTemplate;
    }

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        sce.getServletContext().setAttribute(TracingFilter.SPAN_DECORATORS,
                Collections.singletonList(ServletFilterSpanDecorator.STANDARD_TAGS));
        sce.getServletContext().setAttribute(TracingFilter.SKIP_PATTERN, Pattern.compile("/health"));
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {}
}
