package io.opentracing.contrib.spring.web.starter;

import io.opentracing.contrib.spring.web.client.decorator.RestTemplateHeaderSpanDecorator;
import io.opentracing.contrib.spring.web.starter.properties.WebClientTracingProperties;
import java.util.Objects;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
@ConditionalOnProperty(prefix = WebClientTracingProperties.CONFIGURATION_PREFIX, name = "enabled", matchIfMissing = true)
@ConditionalOnClass(RestTemplate.class)
@AutoConfigureBefore(RestTemplateTracingAutoConfiguration.class)
@EnableConfigurationProperties({ WebClientTracingProperties.class })
public class RestTemplateSpanDecoratorAutoconfiguration {

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = WebClientTracingProperties.CONFIGURATION_PREFIX + "header", name = "enabled", matchIfMissing = true)
    public RestTemplateHeaderSpanDecorator restTemplateHeaderSpanDecorator(WebClientTracingProperties webClientTracingProperties) {
        return new RestTemplateHeaderSpanDecorator(
            webClientTracingProperties.getHeader().getEntries(),
            webClientTracingProperties.getHeader().getBaseTagKey(),
            webClientTracingProperties.getHeader().getOrder());
    }

}
