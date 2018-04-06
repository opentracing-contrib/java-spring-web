package io.opentracing.contrib.spring.web.autoconfig;

import io.opentracing.contrib.spring.web.client.RestTemplateSpanDecorator;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration for tracing of HTTP clients.
 * Supports RestTemplate and AsyncRestTemplate beans.
 *
 * @author Michal Dvorak
 * @see RestTemplateTracingAutoConfiguration
 * @since 4/5/18
 */
@ConfigurationProperties(WebClientTracingProperties.CONFIGURATION_PREFIX)
public class WebClientTracingProperties {

    public static final String CONFIGURATION_PREFIX = WebTracingProperties.CONFIGURATION_PREFIX + ".client";

    /**
     * When set to true (default), it enables automatic tracing of RestTemplate beans, as well as instances created using default RestTemplateBuilder bean.
     * Does not affect instances created manually.
     */
    private boolean enabled = true;
    /**
     * Configures COMPONENT tag value, when default RestTemplateSpanDecorator.StandardTags bean is used.
     * When custom RestTemplateSpanDecorator is provided, this configuration has no effect.
     */
    private String componentName = RestTemplateSpanDecorator.StandardTags.COMPONENT_NAME;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getComponentName() {
        return componentName;
    }

    public void setComponentName(String componentName) {
        this.componentName = componentName;
    }
}
