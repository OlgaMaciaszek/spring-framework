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

package org.springframework.web.reactive.function.client.support;

import java.util.Map;
import java.util.function.Consumer;

import org.springframework.web.client.RestClient;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;
import org.springframework.web.service.registry.AbstractHttpServiceProxyRegistry;
import org.springframework.web.service.registry.HttpServiceConfigurer;
import org.springframework.web.service.registry.HttpServiceGroup;
import org.springframework.web.service.registry.HttpServiceProxyRegistry;

/**
 * Registry for HTTP service proxies backed by {@link WebClient} or {@link RestClient}.
 *
 * @author Rossen Stoyanchev
 * @since 7.0
 */
public class WebClientProxyRegistry extends AbstractHttpServiceProxyRegistry {


	private WebClientProxyRegistry(Map<String, Map<Class<?>, Object>> registrations) {
		super(registrations);
	}


	public static Builder builder(WebClient.Builder baseClientBuilder) {
		return new Builder(baseClientBuilder);
	}


	public final static class Builder extends AbstractBuilder<Builder, WebClient.Builder> {

		private final WebClient.Builder baseClientBuilder;

		private Builder(WebClient.Builder baseClientBuilder) {
			this.baseClientBuilder = baseClientBuilder;
		}

		@Override
		protected WebClientHttpServiceGroup createGroup(String baseUrl) {
			return WebClientHttpServiceGroup.create(baseUrl, this.baseClientBuilder);
		}

		@Override
		public Builder addClient(String baseUrl,
				Consumer<HttpServiceConfigurer> httpServiceConfigurerConsumer,
				Consumer<WebClient.Builder> clientBuilderConsumer,
				Consumer<HttpServiceProxyFactory.Builder> proxyFactoryBuilderConsumer) {

			super.addClient(baseUrl, httpServiceConfigurerConsumer, clientBuilderConsumer, proxyFactoryBuilderConsumer);
			return self();
		}

		@Override
		public Builder addClient(String baseUrl,
				Consumer<HttpServiceConfigurer> httpServiceConfigurer,
				Consumer<WebClient.Builder> clientConfigurer) {

			super.addClient(baseUrl, httpServiceConfigurer, clientConfigurer);
			return self();
		}

		@Override
		public Builder apply(HttpServiceGroup.Configurer<WebClient.Builder> configurer) {
			super.apply(configurer);
			return this;
		}

		@Override
		protected HttpServiceProxyRegistry initRegistry(Map<String, Map<Class<?>, Object>> registrations) {
			return new WebClientProxyRegistry(registrations);
		}
	}

}
