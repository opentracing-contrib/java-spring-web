package io.opentracing.contrib.spring.web.client;

import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;

/**
 * @author Pavol Loffay
 */
public class TracingRestTemplateTest extends AbstractTracingClientTest<RestTemplate> {

    public TracingRestTemplateTest() {
        final RestTemplate restTemplate = new RestTemplate();
        restTemplate.setInterceptors(Collections.<ClientHttpRequestInterceptor>singletonList(
                new TracingRestTemplateInterceptor(mockTracer)));

        client = new Client<RestTemplate>() {
            @Override
            public <T> ResponseEntity<T> getForEntity(String url, Class<T> clazz) {
                return restTemplate.getForEntity(url, clazz);
            }

            @Override
            public RestTemplate template() {
                return restTemplate;
            }
        };

        mockServer = MockRestServiceServer.bindTo(client.template()).build();
    }
}
