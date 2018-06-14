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