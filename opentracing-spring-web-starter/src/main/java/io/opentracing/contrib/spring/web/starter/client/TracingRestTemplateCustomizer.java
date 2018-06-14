package io.opentracing.contrib.spring.web.starter.client;

import io.opentracing.Tracer;
import io.opentracing.contrib.spring.web.client.RestTemplateSpanDecorator;
import io.opentracing.contrib.spring.web.client.TracingRestTemplateInterceptor;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.boot.web.client.RestTemplateCustomizer;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Customizer used by {@link RestTemplateBuilder}.
 * <p>
 * When registered as bean, default {@link RestTemplateBuilder} bean provided by Spring Boot
 * will pick it up.
 *
 * @author Michal Dvorak
 * @see RestTemplate
 * @see RestTemplateBuilder
 */
public class TracingRestTemplateCustomizer implements RestTemplateCustomizer {

    private final Tracer tracer;
    private final List<RestTemplateSpanDecorator> spanDecorators;

    public TracingRestTemplateCustomizer(Tracer tracer, List<RestTemplateSpanDecorator> spanDecorators) {
        this.tracer = Objects.requireNonNull(tracer);
        this.spanDecorators = Objects.requireNonNull(spanDecorators);
    }

    @Override
    public void customize(RestTemplate restTemplate) {
        List<ClientHttpRequestInterceptor> interceptors = restTemplate.getInterceptors();

        for (ClientHttpRequestInterceptor interceptor : interceptors) {
            if (interceptor instanceof TracingRestTemplateInterceptor) {
                return;
            }
        }

        interceptors = new ArrayList<>(interceptors);
        interceptors.add(new TracingRestTemplateInterceptor(tracer, spanDecorators));
        restTemplate.setInterceptors(interceptors);
    }
}
