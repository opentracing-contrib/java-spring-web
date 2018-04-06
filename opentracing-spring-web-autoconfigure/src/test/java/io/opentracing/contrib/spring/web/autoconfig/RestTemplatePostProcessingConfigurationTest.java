package io.opentracing.contrib.spring.web.autoconfig;

import io.opentracing.mock.MockTracer;
import io.opentracing.tag.Tags;
import io.opentracing.util.ThreadLocalScopeManager;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

/**
 * @author Pavol Loffay
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        classes = {RestTemplatePostProcessingConfigurationTest.SpringConfiguration.class},
        properties = "opentracing.spring.web.client.component-name=test-client")
@RunWith(SpringJUnit4ClassRunner.class)
public class RestTemplatePostProcessingConfigurationTest extends AutoConfigurationBaseTest  {

    @Configuration
    @EnableAutoConfiguration
    public static class SpringConfiguration {
        @Bean
        public MockTracer tracer() {
            return new MockTracer(new ThreadLocalScopeManager());
        }

        @Bean
        @Qualifier("foo")
        public RestTemplate restTemplateFoo(RestTemplateBuilder builder) {
            return builder.build();
        }

        @Bean
        @Qualifier("bar")
        public RestTemplate restTemplateBar(RestTemplateBuilder builder) {
            return builder.build();
        }
    }

    @Autowired
    private MockTracer mockTracer;

    @Autowired
    @Qualifier("bar")
    private RestTemplate restTemplate;

    @Before
    public void setUp() {
        mockTracer.reset();
    }

    @Test
    public void testTracingRequest() {
        try {
            restTemplate.getForEntity("http://nonexisting.example.com", String.class);
        } catch (ResourceAccessException ex) {
            //ok UnknownHostException
        }
        Assert.assertEquals(1, mockTracer.finishedSpans().size());
        Assert.assertEquals("test-client", mockTracer.finishedSpans().get(0).tags().get(Tags.COMPONENT.getKey()));
    }
}