package io.opentracing.contrib.spring.web.autoconfig;

import java.util.*;
import java.util.regex.Pattern;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @author Pavol Loffay
 */
@ConfigurationProperties(WebTracingProperties.CONFIGURATION_PREFIX)
public class WebTracingProperties {

    public static final String CONFIGURATION_PREFIX = "opentracing.spring.web";

    public static final Pattern DEFAULT_SKIP_PATTERN = Pattern.compile(
            "/api-docs.*|/autoconfig|/configprops|/dump|/health|/info|/metrics.*|" +
            "/mappings|/swagger.*|.*\\.png|.*\\.css|.*\\.js|.*\\.html|/favicon.ico|/hystrix.stream");

    private Pattern skipPattern = DEFAULT_SKIP_PATTERN;
    private int order = Integer.MIN_VALUE;
    private List<String> urlPatterns = new ArrayList<>(Collections.singletonList("/*"));

    public Pattern getSkipPattern() {
        return skipPattern;
    }

    public void setSkipPattern(Pattern skipPattern) {
        this.skipPattern = skipPattern;
    }

    public List<String> getUrlPatterns() {
        return urlPatterns;
    }

    public void setUrlPatterns(List<String> urlPatterns) {
        this.urlPatterns = urlPatterns;
    }

    public int getOrder() {
        return order;
    }

    public void setOrder(int order) {
        this.order = order;
    }

}
