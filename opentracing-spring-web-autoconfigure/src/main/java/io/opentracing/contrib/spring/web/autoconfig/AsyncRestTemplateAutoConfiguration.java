package io.opentracing.contrib.spring.web.autoconfig;

import io.opentracing.Tracer;
import io.opentracing.contrib.spring.web.client.TracingAsyncRestTemplateInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
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
public class AsyncRestTemplateAutoConfiguration {

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

        interceptors = new ArrayList<>(interceptors);
        interceptors.add(new TracingAsyncRestTemplateInterceptor(tracer));
        restTemplate.setInterceptors(interceptors);
    }
}
