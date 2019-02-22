/*
 * Copyright 2016-2018 The OpenTracing Authors
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

import io.opentracing.Tracer;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.function.Consumer;

/**
 * {@link BeanPostProcessor} for instrumenting {@link WebClient} with tracing.
 *
 * Similar to {@code TraceWebClientBeanPostProcessor} from spring-cloud-sleuth-core.
 *
 * @author Csaba Kos
 */
public class TracingWebClientBeanPostProcessor implements BeanPostProcessor {
	private final Tracer tracer;
	private final List<WebClientSpanDecorator> spanDecorators;

	public TracingWebClientBeanPostProcessor(final Tracer tracer, final List<WebClientSpanDecorator> spanDecorators) {
		this.tracer = tracer;
		this.spanDecorators = spanDecorators;
	}

	@Override
	public Object postProcessBeforeInitialization(final Object bean, final String beanName) throws BeansException {
		return bean;
	}

	@Override
	public Object postProcessAfterInitialization(final Object bean, final String beanName) throws BeansException {
		if (bean instanceof WebClient) {
			final WebClient webClient = (WebClient) bean;
			return webClient.mutate()
					.filters(addTraceExchangeFilterFunctionIfNotPresent()).build();
		} else if (bean instanceof WebClient.Builder) {
			final WebClient.Builder webClientBuilder = (WebClient.Builder) bean;
			return webClientBuilder.filters(addTraceExchangeFilterFunctionIfNotPresent());
		}
		return bean;
	}

	private Consumer<List<ExchangeFilterFunction>> addTraceExchangeFilterFunctionIfNotPresent() {
		return functions -> {
			if (functions.stream()
					.noneMatch(function -> function instanceof TracingExchangeFilterFunction)) {
				functions.add(new TracingExchangeFilterFunction(tracer, spanDecorators));
			}
		};
	}
}

