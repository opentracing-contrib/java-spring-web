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

import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;

/**
 * @author Pavol Loffay
 */
public class TracingRestTemplateTest extends AbstractTracingClientTest {

    public TracingRestTemplateTest() {
        super(tracer -> {
            final RestTemplate restTemplate = new RestTemplate();
            restTemplate.setInterceptors(Collections.singletonList(
                    new TracingRestTemplateInterceptor(tracer)));

            return new Client() {
                @Override
                public <T> ResponseEntity<T> getForEntity(String url, Class<T> clazz) {
                    return restTemplate.getForEntity(url, clazz);
                }
            };
        }, RestTemplateSpanDecorator.StandardTags.COMPONENT_NAME);
    }
}
