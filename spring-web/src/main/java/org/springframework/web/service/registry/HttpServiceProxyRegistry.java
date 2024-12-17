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

import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import org.springframework.lang.Nullable;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

/**
 * Registry for access to HTTP Service proxies grouped by target URL.
 *
 * <p>To create an instance, see
 * {@link org.springframework.web.client.support.RestClientProxyRegistry}, or
 * {@link org.springframework.web.reactive.function.client.support.ReactiveProxyRegistry}.
 *
 * @author Rossen Stoyanchev
 * @since 7.0
 */
public interface HttpServiceProxyRegistry {

	@Nullable
	<S> S getClient(Class<S> httpServiceType);

	@Nullable
	<S> S getClientForBaseUrl(String baseUrl, Class<S> httpServiceType);

	Set<String> getBaseUrls();

	Map<Class<?>, Object> getClientClientsForBaseUrl(String baseUrl);


	/**
	 * Builder for {@code HttpServiceProxyRegistry}.
	 * @param <B> the type of builder
	 * @param <CB> the type of client builder (e.g. RestClient.Builder)
	 */
	interface Builder<B extends Builder<B, CB>, CB> {

		Builder<B, CB> group(String baseUrl);

		Builder<B, CB> httpService(Class<?>... httpServiceTypes);

		Builder<B, CB> configureClient(Consumer<CB> configurer);

		Builder<B, CB> configureProxyFactory(Consumer<HttpServiceProxyFactory.Builder> configurer);

		Builder<B, CB> apply(HttpServiceGroup.Configurer<CB> configurer);

		HttpServiceProxyRegistry build();

	}

}
