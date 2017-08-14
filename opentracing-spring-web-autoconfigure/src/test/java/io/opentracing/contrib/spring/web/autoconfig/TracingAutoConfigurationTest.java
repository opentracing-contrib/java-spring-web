package io.opentracing.contrib.spring.web.autoconfig;

import static org.junit.Assert.assertNotNull;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import io.opentracing.Tracer;

@SpringBootTest(
        classes = {TracingAutoConfigurationTest.SpringConfiguration.class})
@RunWith(SpringJUnit4ClassRunner.class)
public class TracingAutoConfigurationTest {

    @Autowired
    private Tracer tracer;

    @Configuration
    @EnableAutoConfiguration
    public static class SpringConfiguration {
    }

    @Test
    public void testGetTracer() {
        assertNotNull(tracer);
    }
}
