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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import org.jspecify.annotations.Nullable;

import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.env.Environment;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.ResourceLoader;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

/**
 * Base class for {@link HttpServiceProxyRegistry} implementations.
 *
 * @author Rossen Stoyanchev
 * @since 7.0
 */
public abstract class AbstractHttpServiceProxyRegistry implements HttpServiceProxyRegistry {

	private final Map<String, Map<Class<?>, Object>> proxiesByBaseUrl;

	private final MultiValueMap<Class<?>, Object> proxies = new LinkedMultiValueMap<>();


	protected AbstractHttpServiceProxyRegistry(Map<String, Map<Class<?>, Object>> proxiesByBaseUrl) {
		this.proxiesByBaseUrl = proxiesByBaseUrl;
		proxiesByBaseUrl.forEach((baseUrl, proxies) -> proxies.forEach(this.proxies::add));
	}


	@SuppressWarnings("unchecked")
	@Override
	public <S> @Nullable S getClient(Class<S> httpServiceType) {
		List<Object> proxies = this.proxies.get(httpServiceType);
		if (CollectionUtils.isEmpty(proxies)) {
			return null;
		}
		Assert.state(proxies.size() == 1, () -> "More than one proxy for serviceType=" + httpServiceType);
		return (S) proxies.get(0);
	}

	@SuppressWarnings("unchecked")
	@Override
	public <S> @Nullable S getClientForBaseUrl(String baseUrl, Class<S> httpServiceType) {
		Map<Class<?>, Object> map = this.proxiesByBaseUrl.get(baseUrl);
		return (map != null ? (S) map.get(httpServiceType) : null);
	}

	@Override
	public Set<String> getBaseUrls() {
		return this.proxiesByBaseUrl.keySet();
	}

	@Override
	public Map<Class<?>, Object> getClientClientsForBaseUrl(String baseUrl) {
		return this.proxiesByBaseUrl.getOrDefault(baseUrl, new LinkedHashMap<>());
	}


	/**
	 * Base class for {@link HttpServiceProxyRegistry.Builder} implementations.
	 * @param <B> the type of builder
	 * @param <CB> the client builder type
	 */
	protected abstract static class AbstractBuilder<B extends AbstractBuilder<B, CB>, CB> implements Builder<B, CB> {

		private final List<HttpServiceGroup<CB>> groups = new ArrayList<>();

		private @Nullable Environment environment;

		private @Nullable ResourceLoader resourceLoader;

		private @Nullable ClassPathScanningCandidateComponentProvider componentProvider;

		@Override
		public void setEnvironment(Environment environment) {
			this.environment = environment;
		}

		@Override
		public void setResourceLoader(ResourceLoader resourceLoader) {
			this.resourceLoader = resourceLoader;
		}

		protected ClassPathScanningCandidateComponentProvider getComponentProvider() {
			if (this.componentProvider == null) {
				this.environment = (this.environment != null ? this.environment : new StandardEnvironment());
				this.componentProvider = new ClassPathScanningCandidateComponentProvider(false, this.environment);
				this.componentProvider.setResourceLoader(this.resourceLoader);
			}
			return this.componentProvider;
		}

		@Override
		public Builder<B, CB> addClient(String baseUrl,
				Consumer<HttpServiceConfigurer> httpServiceConfigurerConsumer,
				Consumer<CB> clientBuilderConsumer,
				Consumer<HttpServiceProxyFactory.Builder> proxyFactoryBuilderConsumer) {

			AbstractHttpServiceGroup<CB> group = createGroup(baseUrl);
			this.groups.add(group);

			group.configureHttpServices(httpServiceConfigurerConsumer);
			group.configureProxyFactory(proxyFactoryBuilderConsumer);
			group.configureClient(clientBuilderConsumer);

			return self();
		}

		protected abstract AbstractHttpServiceGroup<CB> createGroup(String baseUrl);

		@Override
		public Builder<B, CB> apply(HttpServiceGroup.Configurer<CB> configurer) {
			this.groups.forEach(configurer::configure);
			return self();
		}

		@Override
		public HttpServiceProxyRegistry build() {
			Map<String, Map<Class<?>, Object>> result = new LinkedHashMap<>();
			for (HttpServiceGroup<?> group : this.groups) {
				String baseUrl = group.baseUrl();
				Map<Class<?>, Object> proxies = result.computeIfAbsent(baseUrl, k -> new LinkedHashMap<>());
				group.createProxies().forEach((type, proxy) -> {
					Object previous = proxies.put(type, proxy);
					Assert.state(previous == null, () ->
							"More than one proxy for baseUrl='" + baseUrl + "' and serviceType=" + type);
				});
				result.put(baseUrl, proxies);
			}
			return initRegistry(result);
		}

		protected abstract HttpServiceProxyRegistry initRegistry(Map<String, Map<Class<?>, Object>> registrations);

		@SuppressWarnings("unchecked")
		protected <T extends B> T self() {
			return (T) this;
		}
	}

}
