package io.opentracing.contrib.spring.web.starter.client;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import io.opentracing.Tracer;
import io.opentracing.contrib.spring.web.client.RestTemplateSpanDecorator;
import io.opentracing.contrib.spring.web.client.TracingRestTemplateInterceptor;
import io.opentracing.mock.MockTracer;
import io.opentracing.util.ThreadLocalScopeManager;
import java.util.ArrayList;
import java.util.List;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.WithAssertions;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.web.client.RestTemplate;

@RunWith(MockitoJUnitRunner.class)
public class TracingRestTemplateCustomizerTest implements WithAssertions {

    private RestTemplate restTemplate;

    @Mock
    public Tracer tracer;
    @Mock
    private TracingRestTemplateInterceptor interceptor;
    @Mock
    private ClientHttpRequestInterceptor requestInterceptor;

    private TracingRestTemplateCustomizer customizer;

    @Before
    public void init() {
        restTemplate = new RestTemplate();
        customizer = new TracingRestTemplateCustomizer(tracer, new ArrayList<RestTemplateSpanDecorator>());
    }

    @Test
    public void givenInterceptor_whenCustomizing_thenInterceptorIsAdded() {
        customizer.customize(restTemplate);
        Assertions.assertThat(restTemplate.getInterceptors()).hasOnlyElementsOfType(TracingRestTemplateInterceptor.class);
    }

    @Test
    public void givenAlreadyAddedInterceptor_whenCustomizing_thenInterceptorIsNoAdded() {
        restTemplate.getInterceptors().add(interceptor);
        restTemplate = spy(restTemplate);
        customizer.customize(restTemplate);

        verify(restTemplate, never()).setInterceptors(anyList());
        assertThat(restTemplate.getInterceptors()).containsExactly(interceptor);
    }

    @Test
    public void givenOtherInterceptor_whenCustomizing_thenInterceptorIsAdded() {
        restTemplate.getInterceptors().add(requestInterceptor);
        customizer.customize(restTemplate);

        assertThat(restTemplate.getInterceptors()).hasOnlyElementsOfTypes(TracingRestTemplateInterceptor.class, requestInterceptor.getClass());
    }
}
