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

import io.opentracing.Tracer;
import io.opentracing.contrib.spring.tracer.configuration.TracerAutoConfiguration;
import io.opentracing.contrib.spring.web.client.TracingWebClientBeanPostProcessor;
import io.opentracing.contrib.spring.web.client.WebClientSpanDecorator;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;

/**
 * Instrumentation of {@link WebClient}. It also instruments {@link WebClient.Builder} and all instances created with it.
 *
 * @author Csaba Kos
 */
@Configuration
@ConditionalOnBean(Tracer.class)
@ConditionalOnClass(WebClient.class)
@ConditionalOnProperty(prefix = WebClientTracingProperties.CONFIGURATION_PREFIX, name = "enabled", matchIfMissing = true)
@AutoConfigureAfter(TracerAutoConfiguration.class)
@EnableConfigurationProperties(WebClientTracingProperties.class)
public class WebClientTracingAutoConfiguration {
    @ConditionalOnMissingBean(WebClientSpanDecorator.class)
    @Configuration
    static class DefaultWebClientSpanDecorators {
        @Bean
        WebClientSpanDecorator standardTagsWebClientSpanDecorator() {
            return new WebClientSpanDecorator.StandardTags();
        }
    }

    @Bean
    public static TracingWebClientBeanPostProcessor tracingWebClientBeanPostProcessor(
            final Tracer tracer,
            final ObjectProvider<List<WebClientSpanDecorator>> webClientSpanDecorators
    ) {
        return new TracingWebClientBeanPostProcessor(
                tracer,
                webClientSpanDecorators.getObject()
        );
    }
}
