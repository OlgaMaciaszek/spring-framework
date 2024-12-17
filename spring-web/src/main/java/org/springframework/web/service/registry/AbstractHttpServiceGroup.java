/*
 * Copyright 2002-2024 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.web.service.registry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.web.service.invoker.HttpExchangeAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

/**
 * Base class for {@link HttpServiceGroup} implementations.
 *
 * @author Rossen Stoyanchev
 * @since 7.0
 */
public abstract class AbstractHttpServiceGroup<CB> implements HttpServiceGroup<CB> {

	private final String baseUrl;

	private final CB clientBuilder;

	private Consumer<HttpServiceProxyFactory.Builder> proxyFactoryConfigurer = builder -> {};

	private final List<Class<?>> httpServiceTypes = new ArrayList<>();


	protected AbstractHttpServiceGroup(String baseUrl, CB clientBuilder) {
		this.baseUrl = baseUrl;
		this.clientBuilder = clientBuilder;
	}


	@Override
	public String baseUrl() {
		return this.baseUrl;
	}

	@Override
	public List<Class<?>> httpServices() {
		return this.httpServiceTypes;
	}

	@Override
	public void addHttpService(Class<?>... httpServiceTypes) {
		Collections.addAll(this.httpServiceTypes, httpServiceTypes);
	}

	@Override
	public void configureClient(Consumer<CB> configurer) {
		configurer.accept(this.clientBuilder);
	}

	@Override
	public void configureProxyFactory(Consumer<HttpServiceProxyFactory.Builder> configurer) {
		this.proxyFactoryConfigurer = this.proxyFactoryConfigurer.andThen(configurer);
	}

	@Override
	public Map<Class<?>, Object> createProxies() {
		HttpServiceProxyFactory proxyFactory = initProxyFactory();
		return this.httpServiceTypes.stream()
				.collect(Collectors.toMap(Function.identity(), proxyFactory::createClient));
	}

	private HttpServiceProxyFactory initProxyFactory() {
		HttpExchangeAdapter adapter = createExchangeAdapter(this.clientBuilder);
		HttpServiceProxyFactory.Builder proxyFactoryBuilder = HttpServiceProxyFactory.builderFor(adapter);
		this.proxyFactoryConfigurer.accept(proxyFactoryBuilder);
		return proxyFactoryBuilder.build();
	}

	protected abstract HttpExchangeAdapter createExchangeAdapter(CB clientBuilder);

}
