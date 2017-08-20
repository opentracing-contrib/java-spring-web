package io.opentracing.contrib.spring.web.autoconfig;

import javax.annotation.PostConstruct;

import org.springframework.context.annotation.Configuration;

import io.opentracing.Tracer;
import io.opentracing.util.GlobalTracer;

/**
 * @author Pavol Loffay
 */
@Configuration
public class TracerRegisterAutoConfiguration {

    private final Tracer tracer;

    public TracerRegisterAutoConfiguration(Tracer tracer) {
        this.tracer = tracer;
    }

    @PostConstruct
    public void registerToGlobalTracer() {
        /**
         * Tracer registered in GlobalTracer should be the same as the tracer bean.
         * There can be {@link org.springframework.beans.factory.config.BeanPostProcessor}'s which
         * alters tracer bean, therefore tracer registered to GlobalTracer should be autowired.
         */
        if (!GlobalTracer.isRegistered()) {
            GlobalTracer.register(tracer);
        }
    }

}
