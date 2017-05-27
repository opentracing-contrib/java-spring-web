package io.opentracing.contrib.spring.web.autoconfig;

import io.opentracing.mock.MockTracer;
import io.opentracing.util.ThreadLocalActiveSpanSource;

import org.awaitility.Awaitility;
import org.hamcrest.core.IsEqual;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.web.client.AsyncRestTemplate;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;

/**
 * @author Pavol Loffay
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        classes = {AsyncRestTemplateAutoConfigurationTest.SpringConfiguration.class})
@RunWith(SpringJUnit4ClassRunner.class)
public class AsyncRestTemplateAutoConfigurationTest {

    @Configuration
    @EnableAutoConfiguration
    public static class SpringConfiguration {
        @Bean
        public MockTracer tracer() {
            return new MockTracer(new ThreadLocalActiveSpanSource());
        }

        @Bean
        @Qualifier("foo")
        public AsyncRestTemplate restTemplateFoo() {
            return new AsyncRestTemplate();
        }

        @Bean
        @Qualifier("bar")
        public AsyncRestTemplate restTemplateBar() {
            return new AsyncRestTemplate();
        }
    }

    @Autowired
    private MockTracer mockTracer;

    @Autowired
    @Qualifier("bar")
    private AsyncRestTemplate asyncRestTemplate;

    @Test
    public void testTracingAsyncRequest() throws ExecutionException, InterruptedException {
        asyncRestTemplate.getForEntity("http://example.com", String.class).get();
        Awaitility.await().until(reportedSpansSize(), IsEqual.equalTo(1));
        Assert.assertEquals(1, mockTracer.finishedSpans().size());
    }

    public Callable<Integer> reportedSpansSize() {
        return new Callable<Integer>() {
            @Override
            public Integer call() throws Exception {
                return mockTracer.finishedSpans().size();
            }
        };
    }
}
