package io.opentracing.contrib.spring.web.autoconfig;

import io.opentracing.Tracer;
import io.opentracing.contrib.spring.web.client.TracingAsyncRestTemplateInterceptor;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.AsyncClientHttpRequestInterceptor;
import org.springframework.web.client.AsyncRestTemplate;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * @author Pavol Loffay
 */
@Configuration
@ConditionalOnBean(Tracer.class)
@AutoConfigureAfter(TracerAutoConfiguration.class)
public class AsyncRestTemplateAutoConfiguration {
    private static final Log log = LogFactory.getLog(AsyncRestTemplateAutoConfiguration.class);

    @Autowired(required = false)
    private Set<AsyncRestTemplate> restTemplates;

    @Autowired
    private Tracer tracer;

    @PostConstruct
    public void init() {
        if (restTemplates != null) {
            for (AsyncRestTemplate restTemplate: restTemplates) {
                registerTracingInterceptor(restTemplate);
            }
        }
    }

    private void registerTracingInterceptor(AsyncRestTemplate restTemplate) {
        List<AsyncClientHttpRequestInterceptor> interceptors = restTemplate.getInterceptors();

        for (AsyncClientHttpRequestInterceptor interceptor: interceptors) {
            if (interceptor instanceof TracingAsyncRestTemplateInterceptor) {
                return;
            }
        }

        log.info("Adding " + TracingAsyncRestTemplateInterceptor.class.getSimpleName() + " to async rest template");
        interceptors = new ArrayList<>(interceptors);
        interceptors.add(new TracingAsyncRestTemplateInterceptor(tracer));
        restTemplate.setInterceptors(interceptors);
    }
}
