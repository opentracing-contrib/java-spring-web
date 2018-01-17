package io.opentracing.contrib.spring.web.autoconfig;

import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import io.opentracing.Tracer;
import io.opentracing.noop.NoopSpan;
import io.opentracing.util.GlobalTracer;

@SpringBootTest(
        classes = {TracerAutoConfigurationWithWrapperTest.SpringConfiguration.class,
                TestTracerBeanPostProcessor.class})
@RunWith(SpringJUnit4ClassRunner.class)
public class TracerAutoConfigurationWithWrapperTest extends AutoConfigurationBaseTest {

    @Autowired
    private Tracer tracer;

    @Configuration
    @EnableAutoConfiguration
    public static class SpringConfiguration {
    }

    @Test
    public void testGetAutoWiredTracer() {
        assertTrue(tracer instanceof TestTracerBeanPostProcessor.TracerWrapper);
        // No tracer has actually been provided, but there is a wrapper created
        // in a BeanPostProcessor, so this wrapper around the NoopTracer gets
        // registered with the GlobalTracer.
        assertTrue(GlobalTracer.isRegistered());
        assertTrue(tracer.buildSpan("hello").startManual() instanceof NoopSpan);
    }

}
