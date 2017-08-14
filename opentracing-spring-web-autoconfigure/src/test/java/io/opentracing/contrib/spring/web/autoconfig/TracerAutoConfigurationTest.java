package io.opentracing.contrib.spring.web.autoconfig;

import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import io.opentracing.util.GlobalTracer;

@SpringBootTest(
        classes = {TracerAutoConfigurationTest.SpringConfiguration.class})
@RunWith(SpringJUnit4ClassRunner.class)
public class TracerAutoConfigurationTest {

    @Configuration
    @EnableAutoConfiguration
    public static class SpringConfiguration {
    }

    @Test
    public void testGlobalTracerRegistered() {
        assertTrue(GlobalTracer.isRegistered());
        GlobalTracer.get().buildSpan("TestOp");
        Mockito.verify(TestTracer.tracer).buildSpan("TestOp");
    }
}
