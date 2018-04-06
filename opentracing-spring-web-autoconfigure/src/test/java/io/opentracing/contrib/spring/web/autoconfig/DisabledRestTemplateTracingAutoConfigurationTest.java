package io.opentracing.contrib.spring.web.autoconfig;

import io.opentracing.mock.MockTracer;
import io.opentracing.util.ThreadLocalScopeManager;
import org.awaitility.Awaitility;
import org.hamcrest.core.IsEqual;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.web.client.AsyncRestTemplate;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.Assert.assertNotNull;


/**
 * @author Michal Dvorak
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        classes = {DisabledRestTemplateTracingAutoConfigurationTest.SpringConfiguration.class},
        properties = {"opentracing.spring.web.client.enabled=false"})
@RunWith(SpringJUnit4ClassRunner.class)
@ActiveProfiles("test")
public class DisabledRestTemplateTracingAutoConfigurationTest extends AutoConfigurationBaseTest {

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

    @Autowired(required = false)
    private FilterRegistrationBean tracingFilter;
    @Autowired(required = false)
    private WebMvcConfigurerAdapter tracingHandlerInterceptor;
    @Autowired
    private MockTracer mockTracer;
    @Autowired
    private RestTemplate restTemplate;
    @Autowired
    private AsyncRestTemplate asyncRestTemplate;

    @Before
    public void setUp() {
        mockTracer.reset();
    }

    @Test
    public void testWebConfigurationEnabled() {
        assertNotNull(tracingFilter);
        assertNotNull(tracingHandlerInterceptor);
    }

    @Test
    public void testRestClientNotTracing() {
        try {
            restTemplate.getForEntity("http://nonexisting.example.com", String.class);
        } catch (ResourceAccessException ex) {
            //ok UnknownHostException
        }
        Assert.assertEquals(0, mockTracer.finishedSpans().size());
    }

    @Test
    public void testAsyncRestClientNotTracing() {
        ListenableFuture<ResponseEntity<String>> future = asyncRestTemplate.getForEntity("http://nonexisting.example.com", String.class);

        AtomicBoolean done = AsyncRestTemplatePostProcessingConfigurationTest.addDoneCallback(future);
        Awaitility.await().atMost(500, TimeUnit.MILLISECONDS).untilAtomic(done, IsEqual.equalTo(true));

        Assert.assertEquals(0, mockTracer.finishedSpans().size());
    }
}
