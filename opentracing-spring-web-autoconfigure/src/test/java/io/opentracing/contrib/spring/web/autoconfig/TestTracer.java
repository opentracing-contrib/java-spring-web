package io.opentracing.contrib.spring.web.autoconfig;

import io.opentracing.Tracer;
import io.opentracing.contrib.tracerresolver.TracerResolver;
import io.opentracing.mock.MockTracer;

public class TestTracer extends TracerResolver {

    @Override
    protected Tracer resolve() {
        return new MockTracer();
    }

}
