package io.opentracing.contrib.spring.web.interceptor.itest.boot;

import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.embedded.LocalServerPort;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import io.opentracing.contrib.spring.web.interceptor.itest.common.AbstractBaseITests;

/**
 * @author Pavol Loffay
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        classes = {SpringBootConfiguration.class})
@RunWith(SpringJUnit4ClassRunner.class)
public class BootITest extends AbstractBaseITests {

    @Autowired
    private TestRestTemplate testRestTemplate;

    @LocalServerPort
    private int serverPort;

    @Override
    protected String getUrl(String path) {
        return "http://localhost:" + serverPort + path;
    }

    @Override
    protected TestRestTemplate getRestTemplate() {
        return testRestTemplate;
    }
}
