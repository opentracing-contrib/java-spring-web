/**
 * Copyright 2016-2019 The OpenTracing Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package io.opentracing.contrib.spring.web.client;

import com.github.tomakehurst.wiremock.extension.responsetemplating.ResponseTemplateTransformer;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import io.opentracing.Scope;
import io.opentracing.Span;
import io.opentracing.Tracer;
import io.opentracing.mock.MockSpan;
import io.opentracing.mock.MockTracer;
import io.opentracing.tag.Tags;
import io.opentracing.util.ThreadLocalScopeManager;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.springframework.core.NestedExceptionUtils;
import org.springframework.http.ResponseEntity;

import java.net.UnknownHostException;
import java.util.List;
import java.util.function.Function;

import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.ok;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;

/**
 * @author Pavol Loffay
 */
public abstract class AbstractTracingClientTest {

    protected interface Client {
        <T> ResponseEntity<T> getForEntity(String url, Class<T> type);
    }

    protected final MockTracer mockTracer = new MockTracer(new ThreadLocalScopeManager(),
            MockTracer.Propagator.TEXT_MAP);
    protected final Client client;
    protected final String componentName;

    protected AbstractTracingClientTest(final Function<Tracer, Client> clientFactory, final String componentName) {
        this.client = clientFactory.apply(mockTracer);
        this.componentName = componentName;
    }

    @Rule
    public final WireMockRule wireMockRule = new WireMockRule(wireMockConfig()
            .dynamicPort()
            .extensions(new ResponseTemplateTransformer(true)));

    @Before
    public void before() {
        mockTracer.reset();
    }

    @Test
    public void testStandardTags() {
        final String path = "/foo";
        final String url = wireMockRule.url(path);
        {
            stubFor(get(urlPathEqualTo(path))
                    .willReturn(ok()));
            client.getForEntity(url, String.class);
        }

        List<MockSpan> mockSpans = mockTracer.finishedSpans();
        Assert.assertEquals(1, mockSpans.size());

        MockSpan mockSpan = mockSpans.get(0);
        Assert.assertEquals("GET", mockSpan.operationName());
        Assert.assertEquals(6, mockSpan.tags().size());
        Assert.assertEquals(componentName, mockSpan.tags().get(Tags.COMPONENT.getKey()));
        Assert.assertEquals(Tags.SPAN_KIND_CLIENT, mockSpan.tags().get(Tags.SPAN_KIND.getKey()));
        Assert.assertEquals("GET", mockSpan.tags().get(Tags.HTTP_METHOD.getKey()));
        Assert.assertEquals(url, mockSpan.tags().get(Tags.HTTP_URL.getKey()));
        Assert.assertEquals(200, mockSpan.tags().get(Tags.HTTP_STATUS.getKey()));
        Assert.assertEquals(wireMockRule.port(), mockSpan.tags().get(Tags.PEER_PORT.getKey()));
        Assert.assertEquals(0, mockSpan.logEntries().size());
    }

    @Test
    public void testStandardTagsWithPort() {
        final String path = "/foo";
        final String url = wireMockRule.url(path);
        {
            stubFor(get(urlPathEqualTo(path))
                    .willReturn(ok()));
            client.getForEntity(url, String.class);
        }

        List<MockSpan> mockSpans = mockTracer.finishedSpans();
        Assert.assertEquals(1, mockSpans.size());

        MockSpan mockSpan = mockSpans.get(0);
        Assert.assertEquals("GET", mockSpan.operationName());
        Assert.assertEquals(6, mockSpan.tags().size());
        Assert.assertEquals(componentName, mockSpan.tags().get(Tags.COMPONENT.getKey()));
        Assert.assertEquals(Tags.SPAN_KIND_CLIENT, mockSpan.tags().get(Tags.SPAN_KIND.getKey()));
        Assert.assertEquals("GET", mockSpan.tags().get(Tags.HTTP_METHOD.getKey()));
        Assert.assertEquals(url, mockSpan.tags().get(Tags.HTTP_URL.getKey()));
        Assert.assertEquals(200, mockSpan.tags().get(Tags.HTTP_STATUS.getKey()));
        Assert.assertEquals(wireMockRule.port(), mockSpan.tags().get(Tags.PEER_PORT.getKey()));
        Assert.assertEquals(0, mockSpan.logEntries().size());
    }

    @Test
    public void testInject() {
        final String path = "/foo";
        final String url = wireMockRule.url(path);
        stubFor(get(urlPathEqualTo(path))
                .willReturn(ok()
                        .withHeader("traceId", "{{request.headers.traceid}}")
                        .withHeader("spanId", "{{request.headers.spanid}}")));

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
            Span parent = mockTracer.buildSpan("parent").start();

            final String path = "/foo";
            final String url = wireMockRule.url(path);
            stubFor(get(urlPathEqualTo(path))
                    .willReturn(ok()));
            try (Scope scope = mockTracer.activateSpan(parent)) {
                client.getForEntity(url, String.class);
            } finally {
                parent.finish();
            }
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
            client.getForEntity(url, String.class);
        } catch (RuntimeException ex) {
            Assert.assertTrue(NestedExceptionUtils.getRootCause(ex) instanceof UnknownHostException);
            //ok UnknownHostException
        }

        List<MockSpan> mockSpans = mockTracer.finishedSpans();
        Assert.assertEquals(1, mockSpans.size());

        MockSpan mockSpan = mockSpans.get(0);
        Assert.assertEquals("GET", mockSpan.operationName());
        Assert.assertEquals(5, mockSpan.tags().size());
        Assert.assertEquals(componentName, mockSpan.tags().get(Tags.COMPONENT.getKey()));
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
