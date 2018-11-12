package io.opentracing.contrib.spring.web.client.decorator;

import static org.springframework.util.StringUtils.hasText;

import io.opentracing.Span;
import io.opentracing.contrib.spring.web.client.RestTemplateSpanDecorator;
import io.opentracing.tag.StringTag;
import java.util.ArrayList;
import java.util.List;
import org.springframework.core.Ordered;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpResponse;

/**
 * RestTemplateHeaderSpanDecorator will decorate the span based on HTTP headers of the request.
 * Prior to the request, the headers are compared to the {@link #allowedHeaders} list, if it's part
 * of the provided list, they will be added as {@link StringTag}.
 * The tag format will be a concatenation of {@link #prefix} and {@link HeaderEntry#tag}
 */
public class RestTemplateHeaderSpanDecorator  implements RestTemplateSpanDecorator, Ordered {

    private final String prefix;
    private final List<HeaderEntry> allowedHeaders;
    private final int order;

    public RestTemplateHeaderSpanDecorator(List<HeaderEntry> allowedHeaders) {
        this(allowedHeaders, "http.header.");
    }

    public RestTemplateHeaderSpanDecorator(List<HeaderEntry> allowedHeaders, String prefix, int order) {
        this.allowedHeaders = new ArrayList<>(allowedHeaders);
        this.prefix = hasText(prefix) ? prefix : null;
        this.order = order;
    }

    public RestTemplateHeaderSpanDecorator(List<HeaderEntry> allowedHeaders, String prefix) {
        this(allowedHeaders, prefix, Ordered.LOWEST_PRECEDENCE + 20000);
    }

    @Override
    public void onRequest(HttpRequest request, Span span) {
        for (HeaderEntry headerEntry : allowedHeaders) {
            String headerValue = request.getHeaders().getFirst(headerEntry.getHeader());
            if (hasText(headerValue)) {
                buildTag(headerEntry.getTag()).set(span, headerValue);
            }
        }
    }

    @Override
    public void onResponse(HttpRequest request, ClientHttpResponse response, Span span) {
    }

    @Override
    public void onError(HttpRequest request, Throwable ex, Span span) {
    }

    private StringTag buildTag(String tag) {
        if (!hasText(prefix)) {
            return new StringTag(tag);
        }
        return new StringTag(prefix + tag);
    }

    public String getPrefix() {
        return this.prefix;
    }

    public List<HeaderEntry> getAllowedHeaders() {
        return this.allowedHeaders;
    }

    public int getOrder() {
        return this.order;
    }

    /**
     * HeaderEntry is used to configure {@link RestTemplateHeaderSpanDecorator}
     * {@link #header} is used to check if the header exists using {@link HttpRequest#getHeaders()}
     * {@link #tag} will be used as a {@link StringTag} if {@link #header} is found on the request
     */
    public static class HeaderEntry {
        private final String header;
        private final String tag;

        public HeaderEntry(String header, String tag) {
            this.header = header;
            this.tag = tag;
        }

        public String getHeader() {
            return this.header;
        }

        public String getTag() {
            return this.tag;
        }
    }

}
