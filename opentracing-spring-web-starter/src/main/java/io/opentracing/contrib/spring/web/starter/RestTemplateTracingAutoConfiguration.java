package io.opentracing.contrib.spring.web.starter;

import io.opentracing.Tracer;
import io.opentracing.contrib.spring.tracer.configuration.TracerAutoConfiguration;
import io.opentracing.contrib.spring.web.client.RestTemplateSpanDecorator;
import io.opentracing.contrib.spring.web.client.TracingAsyncRestTemplateInterceptor;
import io.opentracing.contrib.spring.web.starter.client.TracingRestTemplateCustomizer;
import io.opentracing.contrib.spring.web.client.TracingRestTemplateInterceptor;
import io.opentracing.contrib.spring.web.starter.properties.WebClientTracingProperties;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.boot.web.client.RestTemplateCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.http.client.AsyncClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.support.InterceptingAsyncHttpAccessor;
import org.springframework.http.client.support.InterceptingHttpAccessor;
import org.springframework.util.CollectionUtils;
import org.springframework.web.client.AsyncRestTemplate;
import org.springframework.web.client.RestTemplate;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Instrumentation of {@link RestTemplate} and {@link AsyncRestTemplate}.
 * <p>
 * For Spring Boot it also instruments {@link RestTemplateBuilder} and all instances created with it.
 *
 * @author Pavol Loffay
 */
@Configuration
@ConditionalOnBean(Tracer.class)
@ConditionalOnClass(RestTemplate.class)
@ConditionalOnProperty(prefix = WebClientTracingProperties.CONFIGURATION_PREFIX, name = "enabled", matchIfMissing = true)
@AutoConfigureAfter({ TracerAutoConfiguration.class, RestTemplateSpanDecoratorAutoconfiguration.class })
@EnableConfigurationProperties(WebClientTracingProperties.class)
public class RestTemplateTracingAutoConfiguration {

    private static final Log log = LogFactory.getLog(RestTemplateTracingAutoConfiguration.class);


    protected static List<RestTemplateSpanDecorator> decoratorsWithStandardTags(
                                                        ObjectProvider<List<RestTemplateSpanDecorator>> spanDecorators) {

        List<RestTemplateSpanDecorator> allDecorators = new ArrayList<>();
        allDecorators.add(new RestTemplateSpanDecorator.StandardTags());

        List<RestTemplateSpanDecorator> providedDecorators = spanDecorators.getIfAvailable();
        if (!CollectionUtils.isEmpty(providedDecorators)) {
            providedDecorators = new ArrayList<>(providedDecorators);
            AnnotationAwareOrderComparator.sort(providedDecorators);
            allDecorators.addAll(providedDecorators);
        }
        return allDecorators;
    }

    /**
     * Injects {@link TracingRestTemplateInterceptor} into {@link InterceptingHttpAccessor#getInterceptors()}.
     */
    @Configuration
    @ConditionalOnBean(InterceptingHttpAccessor.class)
    public static class RestTemplatePostProcessingConfiguration {

        private final Tracer tracer;
        private final ObjectProvider<List<RestTemplateSpanDecorator>> spanDecorators;
        private final Set<InterceptingHttpAccessor> restTemplates;

        public RestTemplatePostProcessingConfiguration(Tracer tracer,
                                                       ObjectProvider<List<RestTemplateSpanDecorator>> spanDecorators,
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

            log.debug("Adding " + TracingRestTemplateInterceptor.class.getSimpleName() + " to " + restTemplate);
            interceptors = new ArrayList<>(interceptors);
            interceptors.add(new TracingRestTemplateInterceptor(tracer, decoratorsWithStandardTags(spanDecorators)));
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
    @ConditionalOnClass(InterceptingAsyncHttpAccessor.class)
    public static class AsyncRestTemplatePostProcessingConfiguration {

        private final Tracer tracer;
        private final ObjectProvider<List<RestTemplateSpanDecorator>> spanDecorators;
        private final Set<InterceptingAsyncHttpAccessor> restTemplates;

        public AsyncRestTemplatePostProcessingConfiguration(Tracer tracer,
                                                            ObjectProvider<List<RestTemplateSpanDecorator>> spanDecorators,
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

            log.debug("Adding " + TracingAsyncRestTemplateInterceptor.class.getSimpleName() + " to " + restTemplate);
            interceptors = new ArrayList<>(interceptors);
            interceptors.add(new TracingAsyncRestTemplateInterceptor(tracer, decoratorsWithStandardTags(spanDecorators)));
            restTemplate.setInterceptors(interceptors);
        }
    }

    /**
     * Provides {@link TracingRestTemplateCustomizer} bean, which adds {@link TracingRestTemplateInterceptor}
     * into default {@link RestTemplateBuilder} bean.
     * <p>
     * Supported only with Spring Boot.
     */
    @Configuration
    @ConditionalOnClass(RestTemplateCustomizer.class)
    public static class TracingRestTemplateCustomizerConfiguration {

        private final Tracer tracer;
        private final ObjectProvider<List<RestTemplateSpanDecorator>> spanDecorators;

        public TracingRestTemplateCustomizerConfiguration(Tracer tracer,
                                                          ObjectProvider<List<RestTemplateSpanDecorator>> spanDecorators) {
            this.tracer = tracer;
            this.spanDecorators = spanDecorators;
        }

        @Bean
        @ConditionalOnMissingBean(TracingRestTemplateCustomizer.class)
        public TracingRestTemplateCustomizer tracingRestTemplateCustomizer() {
            return new TracingRestTemplateCustomizer(tracer, decoratorsWithStandardTags(spanDecorators));
        }
    }
}
