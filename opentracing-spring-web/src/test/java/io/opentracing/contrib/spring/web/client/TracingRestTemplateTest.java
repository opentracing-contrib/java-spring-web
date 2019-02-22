package io.opentracing.contrib.spring.web.client;

import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;

/**
 * @author Pavol Loffay
 */
public class TracingRestTemplateTest extends AbstractTracingClientTest {

    public TracingRestTemplateTest() {
        super(tracer -> {
            final RestTemplate restTemplate = new RestTemplate();
            restTemplate.setInterceptors(Collections.singletonList(
                    new TracingRestTemplateInterceptor(tracer)));

            return new Client() {
                @Override
                public <T> ResponseEntity<T> getForEntity(String url, Class<T> clazz) {
                    return restTemplate.getForEntity(url, clazz);
                }
            };
        }, RestTemplateSpanDecorator.StandardTags.COMPONENT_NAME);
    }
}
