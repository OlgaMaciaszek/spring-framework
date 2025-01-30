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

import java.util.List;
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

	/**
	 * Look up a proxy by HTTP Service type.
	 * @throws IllegalArgumentException if there is more than one proxy of the
	 * given type, e.g. under different HTTP Service groups.
	 */
	<S> @Nullable S getClient(Class<S> httpServiceType);

	/**
	 * Look up a proxy by qualifying the name of the HTTP Service group that the
	 * proxy is associated with. When a name is not explicitly configured for
	 * a group, by default it is initialized from the baseUrl.
	 */
	<S> @Nullable S getClient(String name, Class<S> httpServiceType);

	/**
	 * Return all HTTP Service groups and the proxies they contain.
	 */
	Set<HttpServiceProxyGroup> getProxyGroups();


	/**
	 * Builder for {@code HttpServiceProxyRegistry}.
	 * @param <B> the type of builder
	 * @param <CB> the type of client builder (e.g. RestClient.Builder)
	 */
	interface Builder<B extends Builder<B, CB>, CB> extends EnvironmentAware, ResourceLoaderAware {

		default Builder<B, CB> addClient(String baseUrl,
				Consumer<HttpServiceConfigurer> httpServiceConfigurer,
				Consumer<CB> clientConfigurer) {

			return addClient(baseUrl, null, httpServiceConfigurer, clientConfigurer, builder -> {});
		}

		Builder<B, CB> addClient(String baseUrl, @Nullable String name,
				Consumer<HttpServiceConfigurer> httpServiceConfigurerConsumer,
				Consumer<CB> clientBuilderConsumer,
				Consumer<HttpServiceProxyFactory.Builder> proxyFactoryBuilderConsumer);

		// TODO: move out of the builder
		Set<InterfaceClientData> discoverClients(List<String> basePackages);

		Builder<B, CB> addClient(InterfaceClientData interfaceClientData,
				Consumer<CB> clientBuilderConsumer);

		Builder<B, CB> apply(HttpServiceGroup.Configurer<CB> configurer);

		HttpServiceProxyRegistry build();

	}

}
