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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.autoconfigure.endpoint.web.WebEndpointProperties;
import org.springframework.boot.actuate.autoconfigure.web.server.ConditionalOnManagementPort;
import org.springframework.boot.actuate.autoconfigure.web.server.ManagementPortType;
import org.springframework.boot.actuate.autoconfigure.web.server.ManagementServerProperties;
import org.springframework.boot.actuate.endpoint.EndpointsSupplier;
import org.springframework.boot.actuate.endpoint.web.ExposableWebEndpoint;
import org.springframework.boot.actuate.endpoint.web.PathMappedEndpoint;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

import io.opentracing.contrib.spring.web.webfilter.SkipPattern;

/**
 * @author Gilles Robert
 */
@Configuration
@ConditionalOnProperty(name = "opentracing.spring.web.enabled", havingValue = "true", matchIfMissing = true)
public class SkipPatternAutoConfiguration {

  @Autowired(required = false)
  private List<SkipPattern> patterns = new ArrayList<>();

  @Bean(name = "skipPattern")
  public Pattern skipPattern() {
    return Pattern.compile(this.patterns
        .stream()
        .map(SkipPattern::pattern)
        .filter(Optional::isPresent).map(Optional::get)
        .map(Pattern::pattern)
        .collect(Collectors.joining("|")));
  }

  @Configuration
  @ConditionalOnClass(ManagementServerProperties.class)
  @ConditionalOnProperty(value = "opentracing.spring.web.ignoreAutoConfiguredSkipPatterns", havingValue = "false", matchIfMissing = true)
  protected static class ManagementSkipPatternProviderConfig {

    static Optional<Pattern> getPatternForManagementServerProperties(
        ManagementServerProperties managementServerProperties) {
      String contextPath = managementServerProperties.getServlet().getContextPath();
      if (StringUtils.hasText(contextPath)) {
        return Optional.of(Pattern.compile(contextPath + ".*"));
      }
      return Optional.empty();
    }

    @Bean
    @ConditionalOnBean(ManagementServerProperties.class)
    public SkipPattern skipPatternForManagementServerProperties(
        final ManagementServerProperties managementServerProperties) {
      return () -> getPatternForManagementServerProperties(managementServerProperties);
    }
  }

  @Configuration
  @ConditionalOnClass( {ServerProperties.class, EndpointsSupplier.class, ExposableWebEndpoint.class})
  @ConditionalOnBean(ServerProperties.class)
  @ConditionalOnProperty(value = "opentracing.spring.web.ignoreAutoConfiguredSkipPatterns", havingValue = "false", matchIfMissing = true)
  protected static class ActuatorSkipPatternProviderConfig {

    static Optional<Pattern> getEndpointsPatterns(String contextPath,
                                                  WebEndpointProperties webEndpointProperties,
                                                  EndpointsSupplier<ExposableWebEndpoint> endpointsSupplier) {
      Collection<ExposableWebEndpoint> endpoints = endpointsSupplier.getEndpoints();

      if (endpoints.isEmpty()) {
        return Optional.empty();
      }

      String pattern = endpoints.stream().map(PathMappedEndpoint::getRootPath)
          .map(path -> path + "|" + path + "/.*").collect(
              Collectors.joining("|",
                  getPathPrefix(contextPath,
                      webEndpointProperties.getBasePath()) + "/(",
                  ")"));
      if (StringUtils.hasText(pattern)) {
        return Optional.of(Pattern.compile(pattern));
      }
      return Optional.empty();
    }

    private static String getPathPrefix(String contextPath, String actuatorBasePath) {
      String result = "";
      if (StringUtils.hasText(contextPath)) {
        result += contextPath;
      }
      if (!actuatorBasePath.equals("/")) {
        result += actuatorBasePath;
      }
      return result;
    }

    @Bean
    @ConditionalOnManagementPort(ManagementPortType.SAME)
    public SkipPattern skipPatternForActuatorEndpointsSamePort(
        final ServerProperties serverProperties,
        final WebEndpointProperties webEndpointProperties,
        final EndpointsSupplier<ExposableWebEndpoint> endpointsSupplier) {
      return () -> getEndpointsPatterns(
          serverProperties.getServlet().getContextPath(), webEndpointProperties,
          endpointsSupplier);
    }

    @Bean
    @ConditionalOnManagementPort(ManagementPortType.DIFFERENT)
    @ConditionalOnProperty(name = "management.server.servlet.context-path", havingValue = "/", matchIfMissing = true)
    public SkipPattern skipPatternForActuatorEndpointsDifferentPort(
        final WebEndpointProperties webEndpointProperties,
        final EndpointsSupplier<ExposableWebEndpoint> endpointsSupplier) {
      return () -> getEndpointsPatterns(null, webEndpointProperties,
          endpointsSupplier);
    }
  }

  @Configuration
  protected static class DefaultSkipPatternConfig {

    private static String combinedPatterns(String skipPattern) {
      String pattern = skipPattern;
      if (!StringUtils.hasText(skipPattern)) {
        pattern = WebTracingProperties.DEFAULT_SKIP_PATTERN;
      }
      return pattern;
    }

    @Bean
    SkipPattern defaultSkipPatternBean(WebTracingProperties webTracingProperties) {
      return () -> Optional.of(Pattern.compile(combinedPatterns(webTracingProperties.getSkipPattern())));
    }
  }

}