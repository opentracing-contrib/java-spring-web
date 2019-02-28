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

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

import static org.junit.Assert.assertNull;

/**
 * @author Gilles Robert
 */
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    classes = {ServerTracingAutoConfigurationDisabledForReactiveTest.SpringConfiguration.class},
    properties = {"spring.main.web-application-type=reactive"})
@RunWith(SpringJUnit4ClassRunner.class)
@ActiveProfiles("test")
public class ServerTracingAutoConfigurationDisabledForReactiveTest extends AutoConfigurationBaseTest {

  @Autowired(required = false)
  @Qualifier("tracingFilter")
  private FilterRegistrationBean tracingFilter;
  @Autowired(required = false)
  @Qualifier("tracingHandlerInterceptor")
  private WebMvcConfigurerAdapter tracingHandlerInterceptor;

  @Configuration
  @EnableAutoConfiguration
  public static class SpringConfiguration {

  }

  @Test
  public void testWebConfigurationDisabled() {
    assertNull(tracingFilter);
    assertNull(tracingHandlerInterceptor);
  }
}
