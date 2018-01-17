package io.opentracing.contrib.spring.web.autoconfig;

import java.util.regex.Pattern;

/**
 * @author Pavol Loffay
 */
public class WebTracingConfiguration {
    public static final Pattern DEFAULT_SKIP_PATTERN = Pattern.compile(
            "/api-docs.*|/autoconfig|/configprops|/dump|/health|/info|/metrics.*|" +
            "/mappings|/swagger.*|.*\\.png|.*\\.css|.*\\.js|.*\\.html|/favicon.ico|/hystrix.stream");

    private Pattern skipPattern;

    private WebTracingConfiguration(Builder builder) {
        this.skipPattern = builder.skipPattern;
    }

    public Pattern getSkipPattern() {
        return skipPattern;
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * This method is provided to enable the {@link WebTracingConfiguration} to be updated during
     * bean post processing. Once the configuration has been used by other layers, then any changes
     * will not take affect.
     *
     * @return A new builder based on the configuration information available in this configuration
     */
    public Builder toBuilder() {
        return new Builder(this);
    }

    public static class Builder {
        private Pattern skipPattern;

        Builder() {
        }

        Builder(WebTracingConfiguration config) {
            this.skipPattern = config.skipPattern;
        }

        public Builder withSkipPattern(Pattern pattern) {
            this.skipPattern = pattern;
            return this;
        }

        public WebTracingConfiguration build() {
            return new WebTracingConfiguration(this);
        }
    }
}
