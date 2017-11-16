package io.opentracing.contrib.spring.web.autoconfig;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import io.opentracing.Tracer;
import io.opentracing.mock.MockSpan;
import io.opentracing.mock.MockTracer;
import io.opentracing.util.GlobalTracer;

@SpringBootTest(
        classes = {TracerAutoConfigurationWithWrapperAndRegisteredTracerTest.SpringConfiguration.class,
                TestTracerBeanPostProcessor.class})
@RunWith(SpringJUnit4ClassRunner.class)
public class TracerAutoConfigurationWithWrapperAndRegisteredTracerTest extends AutoConfigurationBaseTest {

    @BeforeClass
    public static void setGlobalTracer() {
        // Pre-register a tracer with the GlobalTracer
        GlobalTracer.register(new MockTracer());
    }

    @Autowired
    private Tracer tracer;

    @Configuration
    @EnableAutoConfiguration
    public static class SpringConfiguration {
    }

    @Test
    public void testGetAutoWiredTracer() {
        assertNotNull(tracer);
        assertTrue(GlobalTracer.isRegistered());
        assertTrue(tracer.buildSpan("hello").startManual() instanceof MockSpan);
    }

}
