package io.opentracing.contrib.spring.web.interceptor;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static org.junit.Assert.assertNotNull;


@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("classpath:/test-context.xml")
public class TracingHandlerInterceptorIntegrationTest {
    @Autowired
    TracingHandlerInterceptor interceptor;

    @Test
    public void testAutowired() {
        assertNotNull(interceptor);
    }
}
