package io.opentracing.contrib.spring.web.autoconfig;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import io.opentracing.Tracer;
import io.opentracing.mock.MockTracer;
import io.opentracing.util.GlobalTracer;

@SpringBootTest(
        classes = {TracerAutoConfigurationTest.SpringConfiguration.class})
@RunWith(SpringJUnit4ClassRunner.class)
public class TracerAutoConfigurationTest extends AutoConfigurationBaseTest {

    @Autowired
    private Tracer tracer;

    @Configuration
    @EnableAutoConfiguration
    public static class SpringConfiguration {
        @Bean
        public MockTracer tracer() {
            return new MockTracer();
        }
    }

    @Test
    public void testGetAutoWiredTracer() {
        assertTrue(tracer instanceof MockTracer);
        assertTrue(GlobalTracer.isRegistered());
        GlobalTracer.get().buildSpan("hello").startManual().finish();
        assertEquals(1, ((MockTracer)tracer).finishedSpans().size());
    }

}
