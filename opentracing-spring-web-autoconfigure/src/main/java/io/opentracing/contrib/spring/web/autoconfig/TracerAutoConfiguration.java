package io.opentracing.contrib.spring.web.autoconfig;

import java.util.logging.Logger;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.opentracing.Tracer;
import io.opentracing.util.GlobalTracer;

/**
 * @author Pavol Loffay
 */
@Configuration
public class TracerAutoConfiguration {
    private final static Logger log = Logger.getLogger(TracerAutoConfiguration.class.getName());

    @Bean
    @ConditionalOnMissingBean(Tracer.class)
    public Tracer noopTracer() {
        Tracer tracer = GlobalTracer.get();
        log.severe("Tracer bean is not configured! Switching to " + tracer);
        return tracer;
    }
}
