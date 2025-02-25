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

import java.lang.annotation.Annotation;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import org.jspecify.annotations.Nullable;

import org.springframework.core.type.filter.TypeFilter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

/**
 * A grouping of HTTP service types that share a client with the same setup.
 *
 * @author Rossen Stoyanchev
 * @since 7.0
 * @param <G> the concrete HttpServiceGroup subtype
 * @param <CB> the type of client builder (e.g. RestClient.Builder)
 */
public interface HttpServiceGroup<G extends HttpServiceGroup<G, CB>, CB> {

	/**
	 * Return the base URL for the HTTP Service group.
	 */
	String id();

	/**
	 * Return the configured HTTP Service types.
	 */
	Set<Class<?>> httpServiceTypes();

	/**
	 * Set the baseUrl on the underlying client builder. A shortcut for doing the
	 * same directly on the client builder via {@link #configureClient(Consumer)}.
	 */
	G baseUrl(String baseUrl);

	/**
	 * Add the given HTTP service types.
	 */
	G addHttpServiceTypes(Class<?>... httpServiceTypes);

	/**
	 * Scan the classpath for HTTP Service types under one or more base packages,
	 * and with a list of include and exclude filters.
	 * <p>By default, if no include filters are specified, then a filter is added
	 * to search for interfaces annotated with
	 * {@link org.springframework.web.service.annotation.HttpExchange}.
	 * @see org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider
	 */
	G detectHttpServiceTypes(Consumer<ScanSpec> scanConfigurer);

	/**
	 * Callback to configure the underlying HTTP client.
	 */
	G configureClient(Consumer<CB> configurer);

	/**
	 * Callback to configure the {@link HttpServiceProxyFactory} used
	 * to create proxy instances.
	 */
	G configureProxyFactory(Consumer<HttpServiceProxyFactory.Builder> configurer);

	/**
	 * Create proxy instances for all HTTP Service types.
	 */
	void initProxies();

	/**
	 * Return the proxy instance for the given HttpService type.
	 * @param <T> the proxy type
	 * @throws IllegalStateException if {@link #initProxies()} has not been called yet called
	 */
	<T> @Nullable T getClientProxy(Class<T> proxyType);

	/**
	 * Return a Map from HttpService types to proxy instances. The returned Map is
	 * empty before {@link #initProxies()} is called.
	 * @throws IllegalStateException if {@link #initProxies()} has not been called yet called
	 */
	Map<Class<?>, Object> getClientProxyMap();


	/**
	 * Spec to specify HTTP service scan options.
	 */
	interface ScanSpec {

		/**
		 * Add base packages to scan.
		 */
		ScanSpec basePackages(String... basePackages);

		/**
		 * Add base packages through references to classes in those packages.
		 */
		ScanSpec basePackages(Class<?>... basePackageClasses);

		/**
		 * Match HTTP Service types that are assignable to a given type.
		 * @see org.springframework.core.type.filter.AssignableTypeFilter
		 */
		ScanSpec assignableTypes(Class<?>... assignableTypes);

		/**
		 * Match HTTP Service types that have a given annotation, checking inherited
		 * annotations as well.
		 * @see org.springframework.core.type.filter.AnnotationTypeFilter
		 */
		ScanSpec annotation(Class<? extends Annotation> annotation);

		/**
		 * Match HTTP Services types with a regex for fully-qualified class names.
		 * @see org.springframework.core.type.filter.RegexPatternTypeFilter
		 */
		ScanSpec regex(String... expressions);

		/**
		 * Add filters for types that should not be matched.
		 */
		ScanSpec excludeFilters(TypeFilter... typeFilters);
	}

}
