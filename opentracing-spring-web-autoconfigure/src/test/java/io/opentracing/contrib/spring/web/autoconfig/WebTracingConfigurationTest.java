package io.opentracing.contrib.spring.web.autoconfig;

import static org.junit.Assert.*;

import java.util.regex.Pattern;

import org.junit.Test;

public class WebTracingConfigurationTest {

    @Test
    public void testUsing() {
        Pattern pattern = Pattern.compile("/test");
        WebTracingConfiguration config1 = WebTracingConfiguration.builder().withSkipPattern(pattern).build();
        WebTracingConfiguration config2 = WebTracingConfiguration.builder(config1).build();
        assertEquals(pattern.pattern(), config2.getSkipPattern().pattern());
    }

    @Test
    public void testUsingReplaceSkip() {
        Pattern pattern1 = Pattern.compile("/test1");
        Pattern pattern2 = Pattern.compile("/test2");
        WebTracingConfiguration config1 = WebTracingConfiguration.builder().withSkipPattern(pattern1).build();
        WebTracingConfiguration config2 = WebTracingConfiguration.builder(config1).withSkipPattern(pattern2).build();
        assertEquals(pattern2.pattern(), config2.getSkipPattern().pattern());
    }

}
