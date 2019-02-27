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
