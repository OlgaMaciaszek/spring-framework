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

package org.springframework.web.client.support;

import org.springframework.web.client.RestClient;
import org.springframework.web.service.invoker.HttpExchangeAdapter;
import org.springframework.web.service.registry.AbstractHttpServiceGroup;
import org.springframework.web.service.registry.HttpServiceGroup;

/**
 * {@link HttpServiceGroup} for proxies backed by {@link RestClient}.
 *
 * @author Rossen Stoyanchev
 * @since 7.0
 */
public final class RestClientHttpServiceGroup extends AbstractHttpServiceGroup<RestClient.Builder> {


	private RestClientHttpServiceGroup(String baseUrl, RestClient.Builder baseClientBuilder) {
		super(baseUrl, baseClientBuilder.clone());
		configureClient(builder -> builder.baseUrl(baseUrl));
	}


	@Override
	protected HttpExchangeAdapter createExchangeAdapter(RestClient.Builder clientBuilder) {
		RestClient client = clientBuilder.build();
		return RestClientAdapter.create(client);
	}


	public static RestClientHttpServiceGroup create(String baseUrl, RestClient.Builder baseClientBuilder) {
		return new RestClientHttpServiceGroup(baseUrl, baseClientBuilder);
	}


	/**
	 * Configurer for {@link RestClientHttpServiceGroup}.
	 */
	public interface Configurer extends HttpServiceGroup.Configurer<RestClient.Builder> {
	}

}
