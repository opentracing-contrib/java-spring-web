package io.opentracing.contrib.spring.web.autoconfig;

import java.util.regex.Pattern;

/**
 * @author Pavol Loffay
 */
public class WebTracingConfiguration {
    public static final Pattern DEFAULT_SKIP_PATTERN = Pattern.compile("/health|/favicon\\.ico");

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

    public static class Builder {
        private Pattern skipPattern;

        public Builder using(WebTracingConfiguration config) {
            this.skipPattern = config.skipPattern;
            return this;
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
