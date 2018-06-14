package io.opentracing.contrib.spring.web.starter;

import io.opentracing.Span;
import io.opentracing.contrib.spring.web.client.RestTemplateSpanDecorator;
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
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.web.client.AsyncRestTemplate;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author Michal Dvorak
 * @since 4/5/18
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        classes = {
                CustomSpanDecoratorAutoConfigurationTest.SpringConfiguration.class,
                CustomSpanDecoratorAutoConfigurationTest.ClientConfiguration.class,
        })
@RunWith(SpringJUnit4ClassRunner.class)
@ActiveProfiles("test")
public class CustomSpanDecoratorAutoConfigurationTest extends AutoConfigurationBaseTest {

    @Configuration
    @EnableAutoConfiguration
    public static class SpringConfiguration {

        @Bean
        public MockTracer tracer() {
            return new MockTracer(new ThreadLocalScopeManager());
        }

        @Bean
        public RestTemplateSpanDecorator customSpanDecorator() {
            return new RestTemplateSpanDecorator() {
                @Override
                public void onRequest(HttpRequest request, Span span) {
                    span.setTag("custom-test", "foo");
                }

                @Override
                public void onResponse(HttpRequest request, ClientHttpResponse response, Span span) {
                }

                @Override
                public void onError(HttpRequest request, Throwable ex, Span span) {
                }
            };
        }
    }

    @Configuration
    public static class ClientConfiguration {

        @Autowired
        private RestTemplateBuilder builder;

        @Bean
        public RestTemplate restTemplate() {
            return builder.build();
        }

        @Bean
        public AsyncRestTemplate asyncRestTemplate() {
            return new AsyncRestTemplate();
        }
    }

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
    public void testRestClientCustomTracing() {
        try {
            restTemplate.getForEntity("http://nonexisting.example.com", String.class);
        } catch (ResourceAccessException ex) {
            //ok UnknownHostException
        }
        Assert.assertEquals(1, mockTracer.finishedSpans().size());
        Assert.assertEquals("foo", mockTracer.finishedSpans().get(0).tags().get("custom-test"));
    }

    @Test
    public void testAsyncRestClientCustomTracing() {
        ListenableFuture<ResponseEntity<String>> future = asyncRestTemplate.getForEntity("http://nonexisting.example.com", String.class);

        AtomicBoolean done = AsyncRestTemplatePostProcessingConfigurationTest.addDoneCallback(future);
        Awaitility.await().atMost(1000, TimeUnit.MILLISECONDS).untilAtomic(done, IsEqual.equalTo(true));

        Assert.assertEquals(1, mockTracer.finishedSpans().size());
        Assert.assertEquals("foo", mockTracer.finishedSpans().get(0).tags().get("custom-test"));
    }
}
