package io.opentracing.contrib.spring.web.autoconfig;

import org.mockito.Mockito;

import io.opentracing.Tracer;
import io.opentracing.contrib.tracerresolver.TracerResolver;

public class TestTracer extends TracerResolver {

    public static Tracer tracer = Mockito.mock(Tracer.class);

    @Override
    protected Tracer resolve() {
        return tracer;
    }

}
