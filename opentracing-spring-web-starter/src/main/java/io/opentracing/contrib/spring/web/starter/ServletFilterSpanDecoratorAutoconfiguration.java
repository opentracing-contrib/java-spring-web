package io.opentracing.contrib.spring.web.starter;

import io.opentracing.contrib.spring.web.starter.properties.WebTracingProperties;
import io.opentracing.contrib.web.servlet.filter.decorator.ServletFilterHeaderSpanDecorator;
import java.util.Objects;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(prefix = WebTracingProperties.CONFIGURATION_PREFIX, name = "enabled", matchIfMissing = true)
@ConditionalOnWebApplication
@ConditionalOnClass(FilterRegistrationBean.class)
@AutoConfigureBefore(ServerTracingAutoConfiguration.class)
@EnableConfigurationProperties(WebTracingProperties.class)
public class ServletFilterSpanDecoratorAutoconfiguration {

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(value = "tm.opentracing.filter.header.enabled", matchIfMissing = true)
    public ServletFilterHeaderSpanDecorator servletFilterHeaderSpanDecorator(WebTracingProperties webTracingProperties) {
        return new ServletFilterHeaderSpanDecorator(
                webTracingProperties.getHeader().getEntries(),
                webTracingProperties.getHeader().getBaseTagKey());
    }

}