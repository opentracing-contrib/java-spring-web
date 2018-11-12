package io.opentracing.contrib.spring.web.client.decorator;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import io.opentracing.Span;
import io.opentracing.contrib.spring.web.client.decorator.RestTemplateHeaderSpanDecorator.HeaderEntry;
import java.util.ArrayList;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;

@RunWith(MockitoJUnitRunner.class)
public class RestTemplateHeaderSpanDecoratorTest {

    @Mock
    private HttpRequest request;
    @Mock
    private HttpHeaders httpHeaders;

    @Mock
    private Span span;

    private List<HeaderEntry> headerEntries = new ArrayList<>();

    private RestTemplateHeaderSpanDecorator decorator;

    @Before
    public void init() {
        when(request.getHeaders()).thenReturn(httpHeaders);
        headerEntries.add(new HeaderEntry("If-Match", "if-match"));
        decorator = new RestTemplateHeaderSpanDecorator(headerEntries);
    }

    @Test
    public void givenMatchingHeaderEntry_whenOnRequest_thenItShouldAddTag() {
        when(httpHeaders.getFirst("If-Match")).thenReturn("10");

        decorator.onRequest(request, span);
        verify(span).setTag("http.header.if-match", "10");
    }

    @Test
    public void givenNonMatchingHeaderEntry_whenOnRequest_thenItShouldNotAddTag() {
        decorator.onRequest(request, span);
        verifyZeroInteractions(span);
    }

    @Test
    public void givenEmptyMatchingHeaderEntry_whenOnRequest_thenItShouldNotAddTag() {
        when(httpHeaders.getFirst("If-Match")).thenReturn("");

        decorator.onRequest(request, span);
        verifyZeroInteractions(span);
    }

    @Test
    public void givenCustomTag_whenOnRequest_thenItShouldNotAddTag() {
        decorator = new RestTemplateHeaderSpanDecorator(headerEntries, "HEADER.");
        when(httpHeaders.getFirst("If-Match")).thenReturn("10");

        decorator.onRequest(request, span);
        verify(span).setTag("HEADER.if-match", "10");
    }

    @Test
    public void givenEmptyCustomTag_whenOnRequest_thenItShouldNotAddTag() {
        decorator = new RestTemplateHeaderSpanDecorator(headerEntries, "");
        when(httpHeaders.getFirst("If-Match")).thenReturn("10");

        decorator.onRequest(request, span);
        verify(span).setTag("if-match", "10");
    }

}
