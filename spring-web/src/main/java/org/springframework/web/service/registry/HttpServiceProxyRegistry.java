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

import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import org.jspecify.annotations.Nullable;

import org.springframework.context.EnvironmentAware;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

/**
 * Registry for access to HTTP Service proxies grouped by target URL.
 *
 * <p>To create an instance, see
 * {@link org.springframework.web.client.support.RestClientProxyRegistry}, or
 * {@link org.springframework.web.reactive.function.client.support.WebClientProxyRegistry}.
 *
 * @author Rossen Stoyanchev
 * @since 7.0
 */
public interface HttpServiceProxyRegistry {

	<S> @Nullable S getClient(Class<S> httpServiceType);

	<S> @Nullable S getClientForBaseUrl(String baseUrl, Class<S> httpServiceType);

	Set<String> getBaseUrls();

	Map<Class<?>, Object> getClientClientsForBaseUrl(String baseUrl);


	/**
	 * Builder for {@code HttpServiceProxyRegistry}.
	 * @param <B> the type of builder
	 * @param <CB> the type of client builder (e.g. RestClient.Builder)
	 */
	interface Builder<B extends Builder<B, CB>, CB> extends EnvironmentAware, ResourceLoaderAware {

		default Builder<B, CB> addClient(String baseUrl,
				Consumer<HttpServiceConfigurer> httpServiceConfigurer,
				Consumer<CB> clientConfigurer) {

			return addClient(baseUrl, httpServiceConfigurer, clientConfigurer, builder -> {});
		}

		Builder<B, CB> addClient(String baseUrl,
				Consumer<HttpServiceConfigurer> httpServiceConfigurerConsumer,
				Consumer<CB> clientBuilderConsumer,
				Consumer<HttpServiceProxyFactory.Builder> proxyFactoryBuilderConsumer);

		Builder<B, CB> apply(HttpServiceGroup.Configurer<CB> configurer);

		HttpServiceProxyRegistry build();

	}

}
