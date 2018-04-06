package io.opentracing.contrib.spring.web.autoconfig;

import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import io.opentracing.contrib.web.servlet.filter.ServletFilterSpanDecorator;
import org.awaitility.Awaitility;
import org.hamcrest.core.IsEqual;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.opentracing.mock.MockTracer;
import static org.assertj.core.api.Assertions.assertThat;
/**
 * @author Pavol Loffay
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        classes = {ServerTracingAutoConfigurationTest.SpringConfiguration.class},
        properties = "opentracing.spring.web.client.enabled=false")
@RunWith(SpringJUnit4ClassRunner.class)
@ActiveProfiles("test")
public class ServerTracingAutoConfigurationTest extends AutoConfigurationBaseTest  {

    private static CountDownLatch skipCountDownLatch = new CountDownLatch(1);
    private static CountDownLatch excludeCountDownLatch = new CountDownLatch(1);

    @RestController
    @Configuration
    @EnableAutoConfiguration
    public static class SpringConfiguration {
        @Bean
        public MockTracer tracer() {
            return new MockTracer();
        }

        @RequestMapping("/hello")
        public void hello() {
        }

        @RequestMapping("/skip")
        public void skip() {
            skipCountDownLatch.countDown();
        }

        @RequestMapping("/excluded")
        public void excluded() {
            excludeCountDownLatch.countDown();
        }
    }

    @Autowired
    private TestRestTemplate testRestTemplate;

    @Autowired
    private MockTracer mockTracer;

    @Autowired
    @Qualifier("tracingFilter")
    private FilterRegistrationBean tracingFilter;

    @MockBean
    private ServletFilterSpanDecorator mockDecorator1;
    @MockBean
    private ServletFilterSpanDecorator mockDecorator2;

    @Test
    public void testRequestIsTraced() {
        testRestTemplate.getForEntity("/hello", String.class);
        Awaitility.await().until(reportedSpansSize(), IsEqual.equalTo(1));
        assertThat(mockTracer.finishedSpans()).hasSize(1);
    }

    @Test
    public void testOnlyUrlPatternsIsTraced() throws InterruptedException {
        testRestTemplate.getForEntity("/skip", String.class);
        skipCountDownLatch.await();

        assertThat(mockTracer.finishedSpans()).hasSize(0);
        assertThat(Mockito.mockingDetails(mockDecorator1).getInvocations()).hasSize(0);
    }

    @Test
    public void testExcluded() throws InterruptedException {
        testRestTemplate.getForEntity("/excluded", String.class);
        excludeCountDownLatch.await();

        assertThat(mockTracer.finishedSpans()).hasSize(0);
        assertThat(Mockito.mockingDetails(mockDecorator1).getInvocations()).hasSize(0);
        assertThat(Mockito.mockingDetails(mockDecorator2).getInvocations()).hasSize(0);
    }

    @Test
    public void testDecoratedProviderIsUsed() {
        testRestTemplate.getForEntity("/hello", String.class);
        Awaitility.await().until(reportedSpansSize(), IsEqual.equalTo(1));

        assertThat(mockTracer.finishedSpans()).hasSize(1);
        assertThat(Mockito.mockingDetails(mockDecorator1).getInvocations()).hasSize(2);
        assertThat(Mockito.mockingDetails(mockDecorator2).getInvocations()).hasSize(2);
    }

    @Test
    public void testOrderIsUsed() {
        // Ensure the loaded properties (application-test.yml) are used
        assertThat(tracingFilter.getOrder()).isEqualTo(99);
    }
    
    public Callable<Integer> reportedSpansSize() {
        return new Callable<Integer>() {
            @Override
            public Integer call() throws Exception {
                return mockTracer.finishedSpans().size();
            }
        };
    }

    @After()
    public void reset() {
        mockTracer.reset();
        skipCountDownLatch = new CountDownLatch(1);
        excludeCountDownLatch = new CountDownLatch(1);
    }
}
