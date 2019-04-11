/**
 * Copyright 2016-2019 The OpenTracing Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package io.opentracing.contrib.spring.web.starter;

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
            "/api-docs.*|/autoconfig|/configprops|/dump|/health|/info|/metrics.*|/actuator.*|" +
            "/mappings|/swagger.*|.*\\.png|.*\\.css|.*\\.js|.*\\.html|/favicon.ico|/hystrix.stream");

    private boolean enabled = true;
    private Pattern skipPattern = DEFAULT_SKIP_PATTERN;
    private int order = Integer.MIN_VALUE;

    /**
     * List of URL patterns that should be traced.
     *
     * For servlet web stack, the URL pattern syntax is defined in the Servlet spec, under "Specification of Mappings".
     * For reactive (WebFlux), see the documentation of {@link org.springframework.web.util.pattern.PathPattern} for
     * the syntax.
     *
     * By default, the list is empty, which means that all requests will be traced (unless {@link #skipPattern} says
     * otherwise.)
     */
    private List<String> urlPatterns = Collections.emptyList();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

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
