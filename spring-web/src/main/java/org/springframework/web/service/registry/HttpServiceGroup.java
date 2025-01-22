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

import org.springframework.web.service.invoker.HttpServiceProxyFactory;

/**
 * A group of HTTP service types with the same HTTP client setup.
 *
 * @author Rossen Stoyanchev
 * @since 7.0
 * @param <CB> the type of client builder (e.g. RestClient.Builder)
 */
public interface HttpServiceGroup<CB> {

	String baseUrl();

	String name();

	Set<Class<?>> httpServices();

	void configureHttpServices(Consumer<HttpServiceConfigurer> httpServiceTypes);

	void configureClient(Consumer<CB> configurer);

	void configureProxyFactory(Consumer<HttpServiceProxyFactory.Builder> configurer);

	Map<Class<?>, Object> createProxies();


	/**
	 * Callback to configure an {@code HttpServiceGroup}.
	 * @param <CB> the type of client builder (e.g. RestClient.Builder)
	 */
	interface Configurer<CB> {

		void configure(HttpServiceGroup<CB> group);

	}

}
