package io.opentracing.contrib.spring.web.autoconfig;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Field;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import io.opentracing.ActiveSpan;
import io.opentracing.NoopSpan;
import io.opentracing.NoopTracerFactory;
import io.opentracing.Span;
import io.opentracing.SpanContext;
import io.opentracing.Tracer;
import io.opentracing.propagation.Format;
import io.opentracing.util.GlobalTracer;

@SpringBootTest(
        classes = {TracerAutoConfigurationWithWrapperTest.SpringConfiguration.class,
                TracerAutoConfigurationWithWrapperTest.TestTracerBeanPostProcessor.class})
@RunWith(SpringJUnit4ClassRunner.class)
public class TracerAutoConfigurationWithWrapperTest {

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
    }

    @Test
    public void testGetAutoWiredTracer() {
        assertNotNull(tracer);
        // No tracer has actually been provided, but there is a wrapper created
        // in a BeanPostProcessor, so this wrapper around the NoopTracer gets
        // registered with the GlobalTracer.
        assertTrue(GlobalTracer.isRegistered());
        assertTrue(tracer.buildSpan("hello").startManual() instanceof NoopSpan);
    }

    @Configuration
    public static class TestTracerBeanPostProcessor implements BeanPostProcessor {

        @Override
        public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
            if (bean instanceof Tracer) {
                return new TracerWrapper((Tracer)bean);
            }
            return bean;
        }

        @Override
        public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
            return bean;
        }
    }

    public static class TracerWrapper implements Tracer {

        private Tracer wrapped;
        
        public TracerWrapper(Tracer wrapped) {
            this.wrapped = wrapped;
        }

        @Override
        public ActiveSpan activeSpan() {
            return wrapped.activeSpan();
        }

        @Override
        public ActiveSpan makeActive(Span span) {
            return wrapped.makeActive(span);
        }

        @Override
        public SpanBuilder buildSpan(String operationName) {
            return wrapped.buildSpan(operationName);
        }

        @Override
        public <C> void inject(SpanContext spanContext, Format<C> format, C carrier) {
             wrapped.inject(spanContext, format, carrier);
        }

        @Override
        public <C> SpanContext extract(Format<C> format, C carrier) {
            return wrapped.extract(format, carrier);
        }
        
    }
}
