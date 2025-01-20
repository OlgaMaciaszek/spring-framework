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

import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.service.invoker.HttpExchangeAdapter;
import org.springframework.web.service.registry.AbstractHttpServiceGroup;
import org.springframework.web.service.registry.HttpServiceGroup;

/**
 * {@link HttpServiceGroup} for proxies backed by {@link WebClient}.
 *
 * @author Rossen Stoyanchev
 * @since 7.0
 */
public final class WebClientHttpServiceGroup extends AbstractHttpServiceGroup<WebClient.Builder> {


	private WebClientHttpServiceGroup(
			String baseUrl, WebClient.Builder clientBuilder,
			ClassPathScanningCandidateComponentProvider componentProvider) {

		super(baseUrl, clientBuilder, componentProvider);
		configureClient(builder -> builder.baseUrl(baseUrl));
	}


	@Override
	protected HttpExchangeAdapter createExchangeAdapter(WebClient.Builder clientBuilder) {
		WebClient client = clientBuilder.build();
		return WebClientAdapter.create(client);
	}


	public static WebClientHttpServiceGroup create(
			String baseUrl, WebClient.Builder baseClientBuilder,
			ClassPathScanningCandidateComponentProvider componentProvider) {

		return new WebClientHttpServiceGroup(baseUrl, baseClientBuilder, componentProvider);
	}


	/**
	 * Configurer for {@link WebClientHttpServiceGroup}.
	 */
	public interface Configurer extends HttpServiceGroup.Configurer<WebClient.Builder> {
	}

}
