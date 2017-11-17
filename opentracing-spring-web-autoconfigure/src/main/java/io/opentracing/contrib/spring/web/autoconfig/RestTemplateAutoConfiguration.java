package io.opentracing.contrib.spring.web.autoconfig;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.annotation.PostConstruct;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.web.client.RestTemplate;

import io.opentracing.Tracer;
import io.opentracing.contrib.spring.web.client.TracingRestTemplateInterceptor;

/**
 * @author Pavol Loffay
 */
@Configuration
@ConditionalOnBean(Tracer.class)
@AutoConfigureAfter(TracerAutoConfiguration.class)
public class RestTemplateAutoConfiguration {
    private static final Log log = LogFactory.getLog(RestTemplateAutoConfiguration.class);

    @Autowired(required = false)
    private Set<RestTemplate> restTemplates;

    @Autowired
    private Tracer tracer;

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

        log.info("Adding " + TracingRestTemplateInterceptor.class.getSimpleName() + " to rest template");
        interceptors = new ArrayList<>(interceptors);
        interceptors.add(new TracingRestTemplateInterceptor(tracer));
        restTemplate.setInterceptors(interceptors);
    }
}
