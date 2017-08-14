package io.opentracing.contrib.spring.web.autoconfig;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.opentracing.Tracer;
import io.opentracing.contrib.tracerresolver.TracerResolver;
import io.opentracing.util.GlobalTracer;

/**
 * @author Pavol Loffay
 */
@Configuration
public class TracerAutoConfiguration {
    private static final Log log = LogFactory.getLog(TracerAutoConfiguration.class.getName());

    /**
     * This method provides tracer if user did not specify any tracer bean.
     * <p>
     * The order of getting the tracer is:
     * <ol>
     *     <li>Tracer registered in {@link GlobalTracer#register(Tracer)}</li>
     *     <li>Tracer resolved from {@link TracerResolver#resolve()}</li>
     *     <li>Default tracer from {@link GlobalTracer#get()}, which is {@link io.opentracing.NoopTracer}</li>
     * </ol>
     * @return tracer
     */
    @Bean
    @ConditionalOnMissingBean(Tracer.class)
    public Tracer getTracer() {

        if (!GlobalTracer.isRegistered()) {
            Tracer resolvedTracer = TracerResolver.resolveTracer();
            if (resolvedTracer != null) {
                GlobalTracer.register(resolvedTracer);
            }
        } else {
            log.warn("GlobalTracer is already registered. For consistency it is best practice to provide " +
                    "a Tracer bean instead of manually registering it with the GlobalTracer");
        }

        Tracer tracerToReturn = GlobalTracer.get();
        log.warn("Tracer bean is not configured! Switching to " + tracerToReturn);
        return tracerToReturn;
    }
}
