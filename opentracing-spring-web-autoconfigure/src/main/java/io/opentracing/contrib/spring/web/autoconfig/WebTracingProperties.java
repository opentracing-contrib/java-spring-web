package io.opentracing.contrib.spring.web.autoconfig;

import java.util.regex.Pattern;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @author Pavol Loffay
 */
@ConfigurationProperties("opentracing.spring.web")
public class WebTracingProperties {
    public static final Pattern DEFAULT_SKIP_PATTERN = Pattern.compile(
            "/api-docs.*|/autoconfig|/configprops|/dump|/health|/info|/metrics.*|" +
            "/mappings|/swagger.*|.*\\.png|.*\\.css|.*\\.js|.*\\.html|/favicon.ico|/hystrix.stream");

    private Pattern skipPattern = DEFAULT_SKIP_PATTERN;

    public Pattern getSkipPattern() {
        return skipPattern;
    }

    public void setSkipPattern(Pattern skipPattern) {
        this.skipPattern = skipPattern;
    }
}
