package io.opentracing.contrib.spring.web.autoconfig;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.annotation.Configuration;

import io.opentracing.ScopeManager;
import io.opentracing.Span;
import io.opentracing.SpanContext;
import io.opentracing.Tracer;
import io.opentracing.propagation.Format;

@Configuration
public class TestTracerBeanPostProcessor implements BeanPostProcessor {

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

    public static class TracerWrapper implements Tracer {

        private Tracer wrapped;
        
        public TracerWrapper(Tracer wrapped) {
            this.wrapped = wrapped;
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

        @Override
        public ScopeManager scopeManager() {
            return wrapped.scopeManager();
        }

        @Override
        public Span activeSpan() {
            return wrapped.activeSpan();
        }
        
    }
}
