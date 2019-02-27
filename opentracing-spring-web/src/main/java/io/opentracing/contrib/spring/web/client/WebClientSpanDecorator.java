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

import io.opentracing.Span;
import io.opentracing.tag.Tags;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;

import java.util.HashMap;
import java.util.Map;

/**
 * Decorate span by adding tags/logs or operation name change.
 *
 * <p>Do not finish span (otherwise some tags/logs might be missing)!
 *
 * @author Csaba Kos
 */
public interface WebClientSpanDecorator {

    /**
     * Decorate span before before request is executed, e.g. before {@code .onSubscribe()} is called.
     *
     * @param clientRequest request
     * @param span client span
     */
    void onRequest(ClientRequest clientRequest, Span span);

    /**
     * Decorate span after the clientRequest is done, e.g. after {@code .onNext()} is called.
     *
     * @param clientRequest clientRequest
     * @param clientResponse clientResponse
     * @param span span
     */
    void onResponse(ClientRequest clientRequest, ClientResponse clientResponse, Span span);

    /**
     * Decorate span when exception is thrown during clientRequest processing, e.g. when {@code .onError()} is called.
     *
     * @param clientRequest clientRequest
     * @param throwable exception
     * @param span span
     */
    void onError(ClientRequest clientRequest, Throwable throwable, Span span);

    /**
     * Decorate span when the subscription is cancelled.
     *
     * @param clientRequest clientRequest
     * @param span span
     */
    void onCancel(ClientRequest clientRequest, Span span);

    /**
     * This decorator adds set of standard tags to the span.
     */
    class StandardTags implements WebClientSpanDecorator {
        static final String COMPONENT_NAME = "java-spring-webclient";

        @Override
        public void onRequest(final ClientRequest clientRequest, final Span span) {
            Tags.COMPONENT.set(span, COMPONENT_NAME);
            Tags.HTTP_URL.set(span, clientRequest.url().toString());
            Tags.HTTP_METHOD.set(span, clientRequest.method().toString());

            if (clientRequest.url().getPort() != -1) {
                Tags.PEER_PORT.set(span, clientRequest.url().getPort());
            }
        }

        @Override
        public void onResponse(final ClientRequest clientRequest, final ClientResponse clientResponse, final Span span) {
            Tags.HTTP_STATUS.set(span, clientResponse.rawStatusCode());
        }

        @Override
        public void onError(final ClientRequest clientRequest, final Throwable throwable, final Span span) {
            Tags.ERROR.set(span, Boolean.TRUE);
            span.log(errorLogs(throwable));
        }

        @Override
        public void onCancel(final ClientRequest httpRequest, final Span span) {
            final Map<String, Object> logs = new HashMap<>(2);
            logs.put("event", "cancelled");
            logs.put("message", "The subscription was cancelled");
            span.log(logs);
        }

        static Map<String, Object> errorLogs(final Throwable throwable) {
            final Map<String, Object> errorLogs = new HashMap<>(2);
            errorLogs.put("event", Tags.ERROR.getKey());
            errorLogs.put("error.object", throwable);
            return errorLogs;
        }
    }
}
