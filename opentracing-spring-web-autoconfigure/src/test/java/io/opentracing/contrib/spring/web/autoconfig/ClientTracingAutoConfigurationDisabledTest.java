package io.opentracing.contrib.spring.web.autoconfig;

import io.opentracing.mock.MockTracer;
import io.opentracing.util.ThreadLocalScopeManager;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.web.client.AsyncRestTemplate;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

import java.net.UnknownHostException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertNotNull;


/**
 * @author Michal Dvorak
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        classes = {ClientTracingAutoConfigurationDisabledTest.SpringConfiguration.class},
        properties = {"opentracing.spring.web.client.enabled=false"})
@RunWith(SpringJUnit4ClassRunner.class)
@ActiveProfiles("test")
public class ClientTracingAutoConfigurationDisabledTest extends AutoConfigurationBaseTest {

    @Autowired(required = false)
    private FilterRegistrationBean tracingFilter;
    @Autowired(required = false)
    private WebMvcConfigurerAdapter tracingHandlerInterceptor;
    @Autowired
    private SpringConfiguration configuration;

    @Configuration
    @EnableAutoConfiguration
    public static class SpringConfiguration {

        @Bean
        public MockTracer tracer() {
            return new MockTracer(new ThreadLocalScopeManager());
        }

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
    public void testWebConfigurationEnabled() {
        assertNotNull(tracingFilter);
        assertNotNull(tracingHandlerInterceptor);
    }

    @Test
    public void testRestClientNotTracing() {
        try {
            configuration.restTemplate().getForEntity("http://nonexisting.example.com", String.class);
        } catch (ResourceAccessException ex) {
            //ok UnknownHostException
        }
        Assert.assertEquals(0, configuration.tracer().finishedSpans().size());
    }

    @Test
    public void testAsyncRestClientNotTracing() throws Exception {
        try {
            configuration.asyncRestTemplate().getForEntity("http://nonexisting.example.com", String.class).get(5, TimeUnit.SECONDS);
        } catch (ExecutionException ex) {
            //ok UnknownHostException
            if (!(ex.getCause() instanceof UnknownHostException)) {
                throw ex;
            }
        }
        Assert.assertEquals(0, configuration.tracer().finishedSpans().size());
    }
}
