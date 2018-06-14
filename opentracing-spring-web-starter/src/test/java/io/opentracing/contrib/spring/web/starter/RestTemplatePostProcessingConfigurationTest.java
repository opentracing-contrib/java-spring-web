package io.opentracing.contrib.spring.web.starter;

import io.opentracing.mock.MockTracer;
import io.opentracing.util.ThreadLocalScopeManager;
import org.assertj.core.api.Assertions;
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
        classes = {RestTemplatePostProcessingConfigurationTest.SpringConfiguration.class})
@RunWith(SpringJUnit4ClassRunner.class)
public class RestTemplatePostProcessingConfigurationTest extends AutoConfigurationBaseTest {

    @Configuration
    @EnableAutoConfiguration
    public static class SpringConfiguration {
        @Bean
        public MockTracer tracer() {
            return new MockTracer(new ThreadLocalScopeManager());
        }

        @Bean
        @Qualifier("foo")
        public RestTemplate restTemplateFoo() {
            return new RestTemplate();
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
    @Qualifier("foo")
    private RestTemplate fooRestTemplate;
    @Autowired
    @Qualifier("bar")
    private RestTemplate barRestTemplate;
    @Autowired
    private RestTemplateBuilder restTemplateBuilder;

    @Before
    public void setUp() {
        mockTracer.reset();
    }

    @Test
    public void testTracingRequestCustom() {
        try {
            fooRestTemplate.getForEntity("http://nonexisting.example.com", String.class);
        } catch (ResourceAccessException ex) {
            //ok UnknownHostException
        }

        Assertions.assertThat(mockTracer.finishedSpans()).hasSize(1);
    }

    @Test
    public void testTracingRequestBean() {
        try {
            barRestTemplate.getForEntity("http://nonexisting.example.com", String.class);
        } catch (ResourceAccessException ex) {
            //ok UnknownHostException
        }

        // Note: Even that Builder has interceptor and AutoConfig tries to add another one,
        // we still must have only one in the end
        Assertions.assertThat(mockTracer.finishedSpans()).hasSize(1);
    }

    @Test
    public void testTracingFromBuilder() {
        try {
            restTemplateBuilder.build().getForEntity("http://nonexisting.example.com", String.class);
        } catch (ResourceAccessException ex) {
            //ok UnknownHostException
        }

        // Note: Even that Builder has interceptor and AutoConfig tries to add another one,
        // we still must have only one in the end
        Assertions.assertThat(mockTracer.finishedSpans()).hasSize(1);
    }
}
