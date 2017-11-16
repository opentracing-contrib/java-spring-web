package io.opentracing.contrib.spring.web.autoconfig;

import java.lang.reflect.Field;
import org.junit.BeforeClass;
import io.opentracing.NoopTracerFactory;
import io.opentracing.Tracer;
import io.opentracing.util.GlobalTracer;

public class AutoConfigurationBaseTest {

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

}
