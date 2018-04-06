package io.opentracing.contrib.spring.web.autoconfig;

import io.opentracing.Tracer;
import io.opentracing.contrib.spring.web.client.RestTemplateSpanDecorator;
import io.opentracing.contrib.spring.web.client.TracingAsyncRestTemplateInterceptor;
import io.opentracing.contrib.spring.web.client.TracingRestTemplateCustomizer;
import io.opentracing.contrib.spring.web.client.TracingRestTemplateInterceptor;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.client.RestTemplateCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.AsyncClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.support.InterceptingAsyncHttpAccessor;
import org.springframework.http.client.support.InterceptingHttpAccessor;
import org.springframework.web.client.AsyncRestTemplate;
import org.springframework.web.client.RestTemplate;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Instrumentation of {@link RestTemplate} and {@link AsyncRestTemplate}.
 *
 * @author Pavol Loffay
 */
@Configuration
@ConditionalOnBean(Tracer.class)
@ConditionalOnClass(RestTemplate.class)
@ConditionalOnProperty(prefix = WebClientTracingProperties.CONFIGURATION_PREFIX, name = "enabled", matchIfMissing = true)
@AutoConfigureAfter(TracerAutoConfiguration.class)
@EnableConfigurationProperties(WebClientTracingProperties.class)
public class RestTemplateTracingAutoConfiguration {

    private static final Log log = LogFactory.getLog(RestTemplateTracingAutoConfiguration.class);

    /**
     * Provides bean {@link RestTemplateSpanDecorator.StandardTags}.
     */
    @Configuration
    @ConditionalOnMissingBean(RestTemplateSpanDecorator.class)
    public static class StandardTagsConfiguration {

        private final WebClientTracingProperties clientTracingProperties;

        public StandardTagsConfiguration(WebClientTracingProperties clientTracingProperties) {
            this.clientTracingProperties = clientTracingProperties;
        }

        @Bean
        public RestTemplateSpanDecorator.StandardTags standardTagsRestTemplateSpanDecorator() {
            return new RestTemplateSpanDecorator.StandardTags(clientTracingProperties.getComponentName());
        }
    }

    /**
     * Injects {@link TracingRestTemplateInterceptor} into {@link InterceptingHttpAccessor#getInterceptors()}.
     */
    @Configuration
    @ConditionalOnBean(InterceptingHttpAccessor.class)
    public static class RestTemplatePostProcessingConfiguration {

        private final Tracer tracer;
        private final List<RestTemplateSpanDecorator> spanDecorators;
        private final Set<InterceptingHttpAccessor> restTemplates;

        public RestTemplatePostProcessingConfiguration(Tracer tracer,
                                                       List<RestTemplateSpanDecorator> spanDecorators,
                                                       Set<InterceptingHttpAccessor> restTemplates) {
            this.tracer = tracer;
            this.spanDecorators = spanDecorators;
            this.restTemplates = restTemplates;
        }

        @PostConstruct
        public void init() {
            for (InterceptingHttpAccessor restTemplate : restTemplates) {
                registerTracingInterceptor(restTemplate);
            }
        }

        private void registerTracingInterceptor(InterceptingHttpAccessor restTemplate) {
            List<ClientHttpRequestInterceptor> interceptors = restTemplate.getInterceptors();

            for (ClientHttpRequestInterceptor interceptor : interceptors) {
                if (interceptor instanceof TracingRestTemplateInterceptor) {
                    return;
                }
            }

            log.info("Adding " + TracingRestTemplateInterceptor.class.getSimpleName() + " to " + restTemplate);
            interceptors = new ArrayList<>(interceptors);
            interceptors.add(new TracingRestTemplateInterceptor(tracer, spanDecorators));
            restTemplate.setInterceptors(interceptors);
        }
    }

    /**
     * Injects {@link TracingAsyncRestTemplateInterceptor} into {@link InterceptingAsyncHttpAccessor#getInterceptors()}.
     * <p>
     * Note: From Spring Framework 5, {@link org.springframework.web.client.AsyncRestTemplate} is deprecated.
     */
    @Configuration
    @ConditionalOnBean(InterceptingAsyncHttpAccessor.class)
    public static class AsyncRestTemplatePostProcessingConfiguration {

        private final Tracer tracer;
        private final List<RestTemplateSpanDecorator> spanDecorators;
        private final Set<InterceptingAsyncHttpAccessor> restTemplates;

        public AsyncRestTemplatePostProcessingConfiguration(Tracer tracer,
                                                            List<RestTemplateSpanDecorator> spanDecorators,
                                                            Set<InterceptingAsyncHttpAccessor> restTemplates) {
            this.tracer = tracer;
            this.spanDecorators = spanDecorators;
            this.restTemplates = restTemplates;
        }

        @PostConstruct
        public void init() {
            for (InterceptingAsyncHttpAccessor restTemplate : restTemplates) {
                registerTracingInterceptor(restTemplate);
            }
        }

        private void registerTracingInterceptor(InterceptingAsyncHttpAccessor restTemplate) {
            List<AsyncClientHttpRequestInterceptor> interceptors = restTemplate.getInterceptors();

            for (AsyncClientHttpRequestInterceptor interceptor : interceptors) {
                if (interceptor instanceof TracingAsyncRestTemplateInterceptor) {
                    return;
                }
            }

            log.info("Adding " + TracingAsyncRestTemplateInterceptor.class.getSimpleName() + " to " + restTemplate);
            interceptors = new ArrayList<>(interceptors);
            interceptors.add(new TracingAsyncRestTemplateInterceptor(tracer, spanDecorators));
            restTemplate.setInterceptors(interceptors);
        }
    }

    @Configuration
    @ConditionalOnClass(RestTemplateCustomizer.class)
    public static class TracingRestTemplateCustomizerConfiguration {

        private final Tracer tracer;
        private final List<RestTemplateSpanDecorator> spanDecorators;

        public TracingRestTemplateCustomizerConfiguration(Tracer tracer,
                                                          List<RestTemplateSpanDecorator> spanDecorators) {
            this.tracer = tracer;
            this.spanDecorators = spanDecorators;
        }

        @Bean
        public TracingRestTemplateCustomizer tracingRestTemplateCustomizer() {
            return new TracingRestTemplateCustomizer(tracer, spanDecorators);
        }
    }
}
