package io.opentracing.contrib.spring.web.autoconfig;

import java.util.logging.Logger;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
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
    private final static Logger log = Logger.getLogger(TracerAutoConfiguration.class.getName());

    @Autowired
    private Tracer tracer;

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
            log.warning("GlobalTracer is already registered. For consistency it is best practice to provide " +
                    "a Tracer bean instead of manually registering it with the GlobalTracer");
        }

        Tracer tracerToReturn = GlobalTracer.get();
        log.warning("Tracer bean is not configured! Switching to " + tracerToReturn);
        return tracerToReturn;
    }
}
