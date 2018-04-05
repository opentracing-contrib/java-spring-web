package io.opentracing.contrib.spring.web.autoconfig;

import io.opentracing.mock.MockTracer;
import io.opentracing.tag.Tags;
import io.opentracing.util.ThreadLocalScopeManager;
import org.awaitility.Awaitility;
import org.hamcrest.core.IsEqual;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.concurrent.ListenableFutureCallback;
import org.springframework.web.client.AsyncRestTemplate;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author Pavol Loffay
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        classes = {AsyncRestTemplatePostProcessingConfigurationTest.SpringConfiguration.class},
        properties = "opentracing.spring.web.client.component-name=test-async-client")
@RunWith(SpringJUnit4ClassRunner.class)
public class AsyncRestTemplatePostProcessingConfigurationTest extends AutoConfigurationBaseTest {

    @Configuration
    @EnableAutoConfiguration
    public static class SpringConfiguration {
        @Bean
        public MockTracer tracer() {
            return new MockTracer(new ThreadLocalScopeManager());
        }

        @Bean
        @Qualifier("foo")
        public AsyncRestTemplate restTemplateFoo() {
            return new AsyncRestTemplate();
        }

        @Bean
        @Qualifier("bar")
        public AsyncRestTemplate restTemplateBar() {
            return new AsyncRestTemplate();
        }
    }

    @Autowired
    private MockTracer mockTracer;

    @Autowired
    @Qualifier("bar")
    private AsyncRestTemplate asyncRestTemplate;

    @Before
    public void setUp() {
        mockTracer.reset();
    }

    @Test
    public void testTracingAsyncRequest() {
        ListenableFuture<ResponseEntity<String>> future = asyncRestTemplate.getForEntity("http://example.com", String.class);

        AtomicBoolean done = addDoneCallback(future);
        Awaitility.await().atMost(1000, TimeUnit.MILLISECONDS).untilAtomic(done, IsEqual.equalTo(true));

        Assert.assertEquals(1, mockTracer.finishedSpans().size());
        Assert.assertEquals("test-async-client", mockTracer.finishedSpans().get(0).tags().get(Tags.COMPONENT.getKey()));
    }

    public static AtomicBoolean addDoneCallback(ListenableFuture<ResponseEntity<String>> future) {
        final AtomicBoolean done = new AtomicBoolean();

        future.addCallback(new ListenableFutureCallback<ResponseEntity<String>>() {
            @Override
            public void onSuccess(ResponseEntity<String> result) {
                done.set(true);
            }

            @Override
            public void onFailure(Throwable ex) {
                done.set(true);
            }
        });

        return done;
    }
}
