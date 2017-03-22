package io.opentracing.contrib.spring.web;

import java.util.logging.Logger;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.opentracing.NoopTracer;
import io.opentracing.NoopTracerFactory;
import io.opentracing.Tracer;

/**
 * @author Pavol Loffay
 */
@Configuration
public class TracerAutoConfiguration {
    private final static Logger log = Logger.getLogger(TracerAutoConfiguration.class.getName());

    @Bean
    @ConditionalOnMissingBean(Tracer.class)
    public Tracer noopTracer() {
        log.severe("Tracer bean is not configured! Switching to " + NoopTracer.class.getName());
        return NoopTracerFactory.create();
    }
}
