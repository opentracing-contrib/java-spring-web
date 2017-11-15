package io.opentracing.contrib.spring.web.autoconfig;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Field;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import io.opentracing.NoopTracerFactory;
import io.opentracing.Tracer;
import io.opentracing.mock.MockTracer;
import io.opentracing.util.GlobalTracer;
import io.opentracing.util.ThreadLocalActiveSpanSource;

@SpringBootTest(
        classes = {TracerAutoConfigurationTest.SpringConfiguration.class})
@RunWith(SpringJUnit4ClassRunner.class)
public class TracerAutoConfigurationTest {

    // Temporary solution until https://github.com/opentracing/opentracing-java/issues/170 resolved
    private static void _setGlobal(Tracer tracer) {
        try {
            Field globalTracerField = GlobalTracer.class.getDeclaredField("tracer");
            globalTracerField.setAccessible(true);
            globalTracerField.set(null, tracer);
            globalTracerField.setAccessible(false);
        } catch (Exception e) {
            throw new RuntimeException("Error reflecting globalTracer: " + e.getMessage(), e);
        }
    }

    @BeforeClass
    public static void clearGlobalTracer() {
        _setGlobal(NoopTracerFactory.create());
    }

    @Autowired
    private Tracer tracer;

    @Configuration
    @EnableAutoConfiguration
    public static class SpringConfiguration {
        @Bean
        public MockTracer tracer() {
            return new MockTracer(new ThreadLocalActiveSpanSource());
        }
    }

    @Test
    public void testGetAutoWiredTracer() {
        assertNotNull(tracer);
        assertTrue(GlobalTracer.isRegistered());
        GlobalTracer.get().buildSpan("hello").startManual().finish();
        assertEquals(1, ((MockTracer)tracer).finishedSpans().size());
    }

}
