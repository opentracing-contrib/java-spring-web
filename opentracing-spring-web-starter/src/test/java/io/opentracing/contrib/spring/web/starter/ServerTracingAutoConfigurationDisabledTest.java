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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.web.client.AsyncRestTemplate;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertNull;

/**
 * @author Gilles Robert
 */
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    classes = {ServerTracingAutoConfigurationDisabledTest.SpringConfiguration.class},
    properties = {"opentracing.spring.web.enabled=false"})
@RunWith(SpringJUnit4ClassRunner.class)
@ActiveProfiles("test")
public class ServerTracingAutoConfigurationDisabledTest extends AutoConfigurationBaseTest {

  @Autowired(required = false)
  @Qualifier("tracingFilter")
  private FilterRegistrationBean tracingFilter;
  @Autowired(required = false)
  @Qualifier("tracingHandlerInterceptor")
  private WebMvcConfigurerAdapter tracingHandlerInterceptor;

  @Configuration
  @EnableAutoConfiguration
  public static class SpringConfiguration {

    @Bean
    public RestTemplate restTemplate() {
      return new RestTemplate();
    }

    @Bean
    public AsyncRestTemplate asyncRestTemplate() {
      return new AsyncRestTemplate();
    }
  }

  @Test
  public void testWebConfigurationDisabled() {
    assertNull(tracingFilter);
    assertNull(tracingHandlerInterceptor);
  }
}