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
