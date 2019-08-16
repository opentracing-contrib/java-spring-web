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

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Optional;
import java.util.regex.Pattern;

import org.springframework.boot.actuate.autoconfigure.endpoint.web.WebEndpointProperties;
import org.springframework.boot.actuate.autoconfigure.web.server.ManagementServerProperties;
import org.springframework.boot.actuate.endpoint.EndpointId;
import org.springframework.boot.actuate.endpoint.EndpointsSupplier;
import org.springframework.boot.actuate.endpoint.web.ExposableWebEndpoint;
import org.springframework.boot.actuate.endpoint.web.WebOperation;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import org.junit.Test;
import org.junit.runner.RunWith;

import static org.assertj.core.api.BDDAssertions.then;

/**
 * @author Gilles Robert
 */
@RunWith(SpringJUnit4ClassRunner.class)
public class SkipPatternConfigTest {

  @Test
  public void testShouldPickSkipPatternFromWebProperties() {
    WebTracingProperties webTracingProperties = new WebTracingProperties();
    webTracingProperties.setSkipPattern("foo.*|bar.*");
    SkipPatternAutoConfiguration.DefaultSkipPatternConfig defaultSkipPatternConfig =
            new SkipPatternAutoConfiguration.DefaultSkipPatternConfig();
    defaultSkipPatternConfig.webTracingProperties = webTracingProperties;
    Pattern pattern = defaultSkipPatternConfig
        .defaultSkipPatternBean().pattern().get();

    then(pattern.pattern()).isEqualTo("foo.*|bar.*");
  }

  @Test
  public void testShouldReturnEmptyWhenManagementContextHasNoContextPath() {
    Optional<Pattern> pattern = new SkipPatternAutoConfiguration.ManagementSkipPatternProviderConfig()
        .skipPatternForManagementServerProperties(
            new ManagementServerProperties())
        .pattern();

    then(pattern).isEmpty();
  }

  @Test
  public void testShouldReturnManagementContextWithContextPath() {
    ManagementServerProperties properties = new ManagementServerProperties();
    properties.getServlet().setContextPath("foo");

    Optional<Pattern> pattern = new SkipPatternAutoConfiguration.ManagementSkipPatternProviderConfig()
        .skipPatternForManagementServerProperties(properties).pattern();

    then(pattern).isNotEmpty();
    then(pattern.get().pattern()).isEqualTo("foo.*");
  }

  @Test
  public void testShouldReturnEmptyWhenNoEndpoints() {
    EndpointsSupplier<ExposableWebEndpoint> endpointsSupplier = Collections::emptyList;
    Optional<Pattern> pattern = new SkipPatternAutoConfiguration.ActuatorSkipPatternProviderConfig()
        .skipPatternForActuatorEndpointsSamePort(new WebEndpointProperties(), endpointsSupplier)
        .pattern();

    then(pattern).isEmpty();
  }

  @Test
  public void testShouldReturnEndpointsWithoutContextPath() {
    WebEndpointProperties webEndpointProperties = new WebEndpointProperties();
    ServerProperties properties = new ServerProperties();
    properties.getServlet().setContextPath("foo");

    EndpointsSupplier<ExposableWebEndpoint> endpointsSupplier = () -> {
      ExposableWebEndpoint infoEndpoint = createEndpoint("info");
      ExposableWebEndpoint healthEndpoint = createEndpoint("health");

      return Arrays.asList(infoEndpoint, healthEndpoint);
    };

    Optional<Pattern> pattern = new SkipPatternAutoConfiguration.ActuatorSkipPatternProviderConfig()
        .skipPatternForActuatorEndpointsSamePort(webEndpointProperties, endpointsSupplier)
        .pattern();

    then(pattern).isNotEmpty();
    then(pattern.get().pattern())
        .isEqualTo("/actuator/(info|info/.*|health|health/.*)");
  }

  @Test
  public void testShouldReturnEndpointsWithoutContextPathAndBasePathSetToRoot() {
    WebEndpointProperties webEndpointProperties = new WebEndpointProperties();
    webEndpointProperties.setBasePath("/");

    EndpointsSupplier<ExposableWebEndpoint> endpointsSupplier = () -> {
      ExposableWebEndpoint infoEndpoint = createEndpoint("info");
      ExposableWebEndpoint healthEndpoint = createEndpoint("health");

      return Arrays.asList(infoEndpoint, healthEndpoint);
    };

    Optional<Pattern> pattern = new SkipPatternAutoConfiguration.ActuatorSkipPatternProviderConfig()
        .skipPatternForActuatorEndpointsSamePort(webEndpointProperties, endpointsSupplier)
        .pattern();

    then(pattern).isNotEmpty();
    then(pattern.get().pattern()).isEqualTo("/(info|info/.*|health|health/.*)");
  }

  @Test
  public void testShouldReturnEndpointsWithContextPathAndBasePathSetToRoot() {
    WebEndpointProperties webEndpointProperties = new WebEndpointProperties();
    webEndpointProperties.setBasePath("/");

    EndpointsSupplier<ExposableWebEndpoint> endpointsSupplier = () -> {
      ExposableWebEndpoint infoEndpoint = createEndpoint("info");
      ExposableWebEndpoint healthEndpoint = createEndpoint("health");

      return Arrays.asList(infoEndpoint, healthEndpoint);
    };

    Optional<Pattern> pattern = new SkipPatternAutoConfiguration.ActuatorSkipPatternProviderConfig()
        .skipPatternForActuatorEndpointsSamePort(webEndpointProperties, endpointsSupplier)
        .pattern();

    then(pattern).isNotEmpty();
    then(pattern.get().pattern()).isEqualTo("/(info|info/.*|health|health/.*)");
  }

  private ExposableWebEndpoint createEndpoint(final String name) {
    return new ExposableWebEndpoint() {

      @Override
      public String getRootPath() {
        return name;
      }

      @Override
      public EndpointId getEndpointId() {
        return EndpointId.of(name);
      }

      @Override
      public boolean isEnableByDefault() {
        return false;
      }

      @Override
      public Collection<WebOperation> getOperations() {
        return null;
      }
    };
  }
}
