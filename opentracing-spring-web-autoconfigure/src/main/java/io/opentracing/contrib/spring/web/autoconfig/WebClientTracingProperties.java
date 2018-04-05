package io.opentracing.contrib.spring.web.autoconfig;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @author Michal Dvorak
 * @since 4/5/18
 */
@ConfigurationProperties(WebClientTracingProperties.CONFIGURATION_PREFIX)
public class WebClientTracingProperties {

    public static final String CONFIGURATION_PREFIX = WebTracingProperties.CONFIGURATION_PREFIX + ".client";

    private boolean enabled = true;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
