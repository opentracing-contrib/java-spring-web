package io.opentracing.contrib.spring.web.client;

import io.opentracing.Scope;
import io.opentracing.mock.MockSpan;
import io.opentracing.mock.MockTracer;
import io.opentracing.tag.Tags;
import io.opentracing.util.ThreadLocalScopeManager;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.mock.http.client.MockClientHttpResponse;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.ResponseCreator;
import org.springframework.test.web.client.match.MockRestRequestMatchers;
import org.springframework.test.web.client.response.MockRestResponseCreators;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

/**
 * @author Pavol Loffay
 */
public abstract class AbstractTracingClientTest<Template> {

    protected interface Client<Template> {
        <T> ResponseEntity<T> getForEntity(String url, Class<T> type);
        Template template();
    }

    protected final MockTracer mockTracer = new MockTracer(new ThreadLocalScopeManager(),
            MockTracer.Propagator.TEXT_MAP);
    protected MockRestServiceServer mockServer;
    protected Client<Template> client;

    @Before
    public void before() {
        mockTracer.reset();
    }

    @After
    public void after() {
        mockServer.verify();
    }

    @Test
    public void testStandardTags() {
        String url = "http://localhost/foo";
        {
            mockServer.expect(MockRestRequestMatchers.requestTo(url))
                    .andExpect(MockRestRequestMatchers.method(HttpMethod.GET))
                    .andRespond(MockRestResponseCreators.withSuccess());
            client.getForEntity(url, String.class);
        }

        List<MockSpan> mockSpans = mockTracer.finishedSpans();
        Assert.assertEquals(1, mockSpans.size());

        MockSpan mockSpan = mockSpans.get(0);
        Assert.assertEquals("GET", mockSpan.operationName());
        Assert.assertEquals(5, mockSpan.tags().size());
        Assert.assertEquals(RestTemplateSpanDecorator.StandardTags.COMPONENT_NAME,
                mockSpan.tags().get(Tags.COMPONENT.getKey()));
        Assert.assertEquals(Tags.SPAN_KIND_CLIENT, mockSpan.tags().get(Tags.SPAN_KIND.getKey()));
        Assert.assertEquals("GET", mockSpan.tags().get(Tags.HTTP_METHOD.getKey()));
        Assert.assertEquals(url, mockSpan.tags().get(Tags.HTTP_URL.getKey()));
        Assert.assertEquals(200, mockSpan.tags().get(Tags.HTTP_STATUS.getKey()));
        Assert.assertEquals(0, mockSpan.logEntries().size());
    }

    @Test
    public void testStandardTagsWithPort() {
        String url = "http://localhost:4000/foo";
        {
            mockServer.expect(MockRestRequestMatchers.requestTo(url))
                    .andExpect(MockRestRequestMatchers.method(HttpMethod.GET))
                    .andRespond(MockRestResponseCreators.withSuccess());
            client.getForEntity(url, String.class);
        }

        List<MockSpan> mockSpans = mockTracer.finishedSpans();
        Assert.assertEquals(1, mockSpans.size());

        MockSpan mockSpan = mockSpans.get(0);
        Assert.assertEquals("GET", mockSpan.operationName());
        Assert.assertEquals(6, mockSpan.tags().size());
        Assert.assertEquals(RestTemplateSpanDecorator.StandardTags.COMPONENT_NAME,
                mockSpan.tags().get(Tags.COMPONENT.getKey()));
        Assert.assertEquals(Tags.SPAN_KIND_CLIENT, mockSpan.tags().get(Tags.SPAN_KIND.getKey()));
        Assert.assertEquals("GET", mockSpan.tags().get(Tags.HTTP_METHOD.getKey()));
        Assert.assertEquals(url, mockSpan.tags().get(Tags.HTTP_URL.getKey()));
        Assert.assertEquals(200, mockSpan.tags().get(Tags.HTTP_STATUS.getKey()));
        Assert.assertEquals(4000, mockSpan.tags().get(Tags.PEER_PORT.getKey()));
        Assert.assertEquals(0, mockSpan.logEntries().size());
    }

    @Test
    public void testInject() {
        String url = "http://localhost:4000/foo";
        mockServer.expect(MockRestRequestMatchers.requestTo(url))
                .andExpect(MockRestRequestMatchers.method(HttpMethod.GET))
                .andRespond(new ResponseCreator() {
                    @Override
                    public ClientHttpResponse createResponse(ClientHttpRequest request) throws IOException {
                        MockClientHttpResponse response =
                                new MockClientHttpResponse(new byte[1], HttpStatus.OK);

                        response.getHeaders().add("traceId", request.getHeaders()
                                .getFirst("traceId"));
                        response.getHeaders().add("spanId", request.getHeaders()
                                .getFirst("spanId"));
                        return response;
                    }
                });

        ResponseEntity<String> responseEntity = client.getForEntity(url, String.class);

        List<MockSpan> mockSpans = mockTracer.finishedSpans();
        Assert.assertEquals(1, mockSpans.size());
        Assert.assertEquals(mockSpans.get(0).context().traceId(),
                Long.parseLong(responseEntity.getHeaders().getFirst("traceId")));
        Assert.assertEquals(mockSpans.get(0).context().spanId(),
                Long.parseLong(responseEntity.getHeaders().getFirst("spanId")));
    }

    @Test
    public void testParentSpan() {
        {
            Scope parent = mockTracer.buildSpan("parent").startActive(true);

            String url = "http://localhost/foo";
            mockServer.expect(MockRestRequestMatchers.requestTo(url))
                    .andExpect(MockRestRequestMatchers.method(HttpMethod.GET))
                    .andRespond(MockRestResponseCreators.withSuccess());
            client.getForEntity(url, String.class);

            parent.close();
        }

        List<MockSpan> mockSpans = mockTracer.finishedSpans();
        Assert.assertEquals(2, mockSpans.size());
        Assert.assertEquals(mockSpans.get(0).parentId(), mockSpans.get(1).context().spanId());
        Assert.assertEquals(mockSpans.get(0).context().traceId(), mockSpans.get(1).context().traceId());
    }

    @Test
    public void testErrorUnknownHostException() {
        String url = "http://nonexisting.example.com";
        try {
            RestTemplate restTemplate = new RestTemplate();
            restTemplate.setInterceptors(Collections.<ClientHttpRequestInterceptor>singletonList(
                    new TracingRestTemplateInterceptor(mockTracer)));
            restTemplate.getForEntity(url, String.class);
        } catch (ResourceAccessException ex) {
            //ok UnknownHostException
        }

        List<MockSpan> mockSpans = mockTracer.finishedSpans();
        Assert.assertEquals(1, mockSpans.size());

        MockSpan mockSpan = mockSpans.get(0);
        Assert.assertEquals("GET", mockSpan.operationName());
        Assert.assertEquals(5, mockSpan.tags().size());
        Assert.assertEquals(RestTemplateSpanDecorator.StandardTags.COMPONENT_NAME,
                mockSpan.tags().get(Tags.COMPONENT.getKey()));
        Assert.assertEquals(Tags.SPAN_KIND_CLIENT, mockSpan.tags().get(Tags.SPAN_KIND.getKey()));
        Assert.assertEquals("GET", mockSpan.tags().get(Tags.HTTP_METHOD.getKey()));
        Assert.assertEquals(url, mockSpan.tags().get(Tags.HTTP_URL.getKey()));
        Assert.assertEquals(Boolean.TRUE, mockSpan.tags().get(Tags.ERROR.getKey()));

        Assert.assertEquals(1, mockSpan.logEntries().size());
        Assert.assertEquals(2, mockSpan.logEntries().get(0).fields().size());
        Assert.assertEquals(Tags.ERROR.getKey(), mockSpan.logEntries().get(0).fields().get("event"));
        Assert.assertNotNull(mockSpan.logEntries().get(0).fields().get("error.object"));
    }
}
