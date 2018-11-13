package io.opentracing.contrib.spring.web.starter.properties;

import io.opentracing.contrib.spring.web.client.decorator.RestTemplateHeaderSpanDecorator.HeaderEntry;
import io.opentracing.contrib.spring.web.starter.RestTemplateTracingAutoConfiguration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.Ordered;

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
     * To exclude bean by name from the Post processing
     */
    private Set<String> excludedBeans = new HashSet<>();

    private HeaderProperties header = new ClientHeaderProperties();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Set<String> getExcludedBeans() {
        return excludedBeans;
    }

    public void setExcludedBeans(Set<String> excludedBeans) {
        this.excludedBeans = excludedBeans;
    }

    public HeaderProperties getHeader() {
        return header;
    }

    public void setHeader(HeaderProperties header) {
        this.header = header;
    }
}
