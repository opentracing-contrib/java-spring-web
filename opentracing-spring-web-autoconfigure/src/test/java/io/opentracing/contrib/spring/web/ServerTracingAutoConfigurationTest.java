package io.opentracing.contrib.spring.web;

import java.util.concurrent.Callable;

import org.hamcrest.core.IsEqual;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.jayway.awaitility.Awaitility;

import io.opentracing.mock.MockTracer;

/**
 * @author Pavol Loffay
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        classes = {ServerTracingAutoConfigurationTest.SpringConfiguration.class})
@RunWith(SpringJUnit4ClassRunner.class)
public class ServerTracingAutoConfigurationTest {

    @Configuration
    @EnableAutoConfiguration
    public static class SpringConfiguration {
        @Bean
        public MockTracer tracer() {
            return new MockTracer();
        }
    }

    @Autowired
    private TestRestTemplate testRestTemplate;

    @Autowired
    private MockTracer mockTracer;

    @Test
    public void testRequestIsTraced() {
        testRestTemplate.getForEntity("/hello", String.class);
        Awaitility.await().until(reportedSpansSize(), IsEqual.equalTo(2));

        Assert.assertEquals(2, mockTracer.finishedSpans().size());
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
