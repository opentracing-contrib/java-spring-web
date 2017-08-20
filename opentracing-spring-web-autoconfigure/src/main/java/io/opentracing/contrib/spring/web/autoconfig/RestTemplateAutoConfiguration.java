package io.opentracing.contrib.spring.web.autoconfig;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.web.client.RestTemplate;

import io.opentracing.Tracer;
import io.opentracing.contrib.spring.web.client.TracingRestTemplateInterceptor;

/**
 * @author Pavol Loffay
 */
@Configuration
public class RestTemplateAutoConfiguration {

    private final Set<RestTemplate> restTemplates;

    private final Tracer tracer;

    public RestTemplateAutoConfiguration(ObjectProvider<Set<RestTemplate>> restTemplates, Tracer tracer) {
        this.restTemplates = restTemplates.getIfAvailable();
        this.tracer = tracer;
    }

    @PostConstruct
    public void init() {
        if (restTemplates != null) {
            for (RestTemplate restTemplate: restTemplates) {
                registerTracingInterceptor(restTemplate);
            }
        }
    }

    private void registerTracingInterceptor(RestTemplate restTemplate) {
        List<ClientHttpRequestInterceptor> interceptors = restTemplate.getInterceptors();

        for (ClientHttpRequestInterceptor interceptor: interceptors) {
            if (interceptor instanceof TracingRestTemplateInterceptor) {
                return;
            }
        }

        interceptors = new ArrayList<>(interceptors);
        interceptors.add(new TracingRestTemplateInterceptor(tracer));
        restTemplate.setInterceptors(interceptors);
    }
}
