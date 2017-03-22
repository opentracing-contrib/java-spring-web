package io.opentracing.contrib.spring.web.client;

import java.util.Iterator;
import java.util.Map;

import org.springframework.http.HttpHeaders;

import io.opentracing.propagation.TextMap;

/**
 * @author Pavol Loffay
 */
public class HttpHeadersCarrier implements TextMap {

    private HttpHeaders httpHeaders;

    HttpHeadersCarrier(HttpHeaders httpHeaders)  {
        this.httpHeaders = httpHeaders;
    }

    @Override
    public void put(String key, String value) {
        httpHeaders.add(key, value);
    }

    @Override
    public Iterator<Map.Entry<String, String>> iterator() {
        throw new UnsupportedOperationException("Should be used only with tracer#inject()");
    }
}
