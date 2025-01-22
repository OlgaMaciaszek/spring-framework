/*
 * Copyright 2002-2025 the original author or authors.
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

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.core.type.filter.TypeFilter;
import org.springframework.util.ClassUtils;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.invoker.HttpExchangeAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

/**
 * Base class for {@link HttpServiceGroup} implementations.
 *
 * @author Rossen Stoyanchev
 * @since 7.0
 * @param <CB> the client builder type
 */
public abstract class AbstractHttpServiceGroup<CB> implements HttpServiceGroup<CB> {

	private static final AnnotationTypeFilter httpExchangeAnnotationFilter =
			new AnnotationTypeFilter(HttpExchange.class, true);


	private final String baseUrl;

	private final String name;

	private final CB clientBuilder;

	private Consumer<HttpServiceProxyFactory.Builder> proxyFactoryConfigurer = builder -> {};

	private final Set<Class<?>> httpServiceTypes = new LinkedHashSet<>();

	private final DefaultHttpServiceConfigurer httpServiceConfigurer;


	protected AbstractHttpServiceGroup(
			String baseUrl, String name, CB clientBuilder,
			ClassPathScanningCandidateComponentProvider componentProvider) {

		this.baseUrl = baseUrl;
		this.name = name;
		this.clientBuilder = clientBuilder;
		this.httpServiceConfigurer = new DefaultHttpServiceConfigurer(componentProvider);
	}


	@Override
	public String baseUrl() {
		return this.baseUrl;
	}

	@Override
	public String name() {
		return this.name;
	}

	@Override
	public Set<Class<?>> httpServices() {
		return this.httpServiceTypes;
	}

	@Override
	public void configureHttpServices(Consumer<HttpServiceConfigurer> httpServicesConsumer) {
		httpServicesConsumer.accept(this.httpServiceConfigurer);
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


	private class DefaultHttpServiceConfigurer implements HttpServiceConfigurer {

		private final ClassPathScanningCandidateComponentProvider componentProvider;

		public DefaultHttpServiceConfigurer(ClassPathScanningCandidateComponentProvider componentProvider) {
			this.componentProvider = componentProvider;
		}

		@Override
		public HttpServiceConfigurer addServiceTypes(Class<?>... types) {
			Collections.addAll(AbstractHttpServiceGroup.this.httpServiceTypes, types);
			return this;
		}

		@Override
		public HttpServiceConfigurer discoverServiceTypes(
				String basePackage, List<TypeFilter> includeFilters, List<TypeFilter> excludeFilters) {

			includeFilters.add(httpExchangeAnnotationFilter);
			includeFilters.forEach(this.componentProvider::addIncludeFilter);
			excludeFilters.forEach(this.componentProvider::addExcludeFilter);

			this.componentProvider.findCandidateComponents(basePackage).forEach(definition -> {
				String className = definition.getBeanClassName();
				if (className == null) {
					return;
				}
				try {
					Class<?> clazz = ClassUtils.forName(className, AbstractHttpServiceGroup.class.getClassLoader());
					AbstractHttpServiceGroup.this.httpServiceTypes.add(clazz);
				}
				catch (ClassNotFoundException ex) {
					throw new RuntimeException("Failed to find class name '" + className + "'", ex);
				}
			});

			return this;
		}
	}

}
