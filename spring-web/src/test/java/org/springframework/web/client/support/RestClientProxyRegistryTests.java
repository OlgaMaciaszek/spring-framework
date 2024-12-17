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

import java.io.IOException;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.web.client.RestClient;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.registry.HttpServiceGroup;
import org.springframework.web.service.registry.HttpServiceProxyRegistry;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.web.client.support.RestClientProxyRegistryTests.UserGroupConfigurer.auth;

/**
 * Unit tests for {@link RestClientProxyRegistry}.
 * @author Rossen Stoyanchev
 */
public class RestClientProxyRegistryTests {

	private final MockWebServer server1 = new MockWebServer();

	private final MockWebServer server2 = new MockWebServer();


	@SuppressWarnings("ConstantValue")
	@AfterEach
	void shutdown() throws IOException {
		if (this.server1 != null) {
			this.server1.shutdown();
		}
		if (this.server2 != null) {
			this.server2.shutdown();
		}
	}


	@Test
	void basic() throws Exception {

		// Base builder with default header
		RestClient.Builder baseClientBuilder = RestClient.builder().defaultHeader("Base-Header", "h0");
		String baseUrl = server1.url("/").toString();

		HttpServiceProxyRegistry registry =
				RestClientProxyRegistry.builder(baseClientBuilder)
						.group(baseUrl)
						.httpService(GreetingService.class)
						.configureClient(clientBuilder -> clientBuilder.defaultHeader("Some-Header", "h1"))
						.build();

		GreetingService service = registry.getClientForBaseUrl(baseUrl, GreetingService.class);

		MockResponse response = new MockResponse();
		response.setHeader("Content-Type", "text/plain").setBody("Hello!");
		this.server1.enqueue(response);

		assertThat(service.getGreeting()).isEqualTo("Hello!");

		RecordedRequest request1 = this.server1.takeRequest();
		assertThat(request1.getHeader("Base-Header")).isEqualTo("h0");
		assertThat(request1.getHeader("Some-Header")).isEqualTo("h1");
	}

	@Test
	void twoGroups() throws Exception {

		// Base builder with default header
		RestClient.Builder baseClientBuilder = RestClient.builder().defaultHeader("Base-Header", "h0");

		String baseUrl1 = server1.url("/").toString();
		String baseUrl2 = server2.url("/").toString();

		HttpServiceProxyRegistry registry =
				RestClientProxyRegistry.builder(baseClientBuilder)
						.group(baseUrl1)
						.httpService(GreetingService.class)
						.configureClient(clientBuilder -> clientBuilder.defaultHeader("Some-Header", "h1"))
						.group(baseUrl2)
						.httpService(GreetingService.class)
						.configureClient(clientBuilder -> clientBuilder.defaultHeader("Some-Header", "h2"))
						.build();

		GreetingService g1 = registry.getClientForBaseUrl(baseUrl1, GreetingService.class);
		GreetingService g2 = registry.getClientForBaseUrl(baseUrl2, GreetingService.class);

		MockResponse response = new MockResponse();
		response.setHeader("Content-Type", "text/plain").setBody("Hello!");
		this.server1.enqueue(response);
		this.server2.enqueue(response);

		assertThat(g1.getGreeting()).isEqualTo("Hello!");
		assertThat(g2.getGreeting()).isEqualTo("Hello!");

		RecordedRequest request1 = this.server1.takeRequest();
		assertThat(request1.getHeader("Base-Header")).isEqualTo("h0");
		assertThat(request1.getHeader("Some-Header")).isEqualTo("h1");

		RecordedRequest request2 = this.server2.takeRequest();
		assertThat(request2.getHeader("Base-Header")).isEqualTo("h0");
		assertThat(request2.getHeader("Some-Header")).isEqualTo("h2");
	}

	@Test
	void customDsl() throws Exception {

		RestClient.Builder baseClientBuilder = RestClient.builder();

		String baseUrl1 = server1.url("/").toString();
		String baseUrl2 = server2.url("/").toString();

		RestClientProxyRegistry.Builder registryBuilder =
				RestClientProxyRegistry.builder(baseClientBuilder)
						.group(baseUrl1)
						.httpService(GreetingService.class)
						.configureClient(clientBuilder -> clientBuilder.defaultHeader("Some-Header", "h1"))
						.group(baseUrl2)
						.httpService(GreetingService.class)
						.configureClient(clientBuilder -> clientBuilder.defaultHeader("Some-Header", "h2"));

		registryBuilder.apply(auth().user("john").password("123"));

		HttpServiceProxyRegistry registry = registryBuilder.build();

		GreetingService g1 = registry.getClientForBaseUrl(baseUrl1, GreetingService.class);
		GreetingService g2 = registry.getClientForBaseUrl(baseUrl2, GreetingService.class);

		MockResponse response = new MockResponse();
		response.setHeader("Content-Type", "text/plain").setBody("Hello!");
		this.server1.enqueue(response);
		this.server2.enqueue(response);

		assertThat(g1.getGreeting()).isEqualTo("Hello!");
		assertThat(g2.getGreeting()).isEqualTo("Hello!");

		RecordedRequest request = this.server1.takeRequest();
		assertThat(request.getHeader("Some-Header")).isEqualTo("h1");
		assertThat(request.getHeader("Authorization")).isEqualTo("Basic am9objoxMjM=");

		request = this.server2.takeRequest();
		assertThat(request.getHeader("Some-Header")).isEqualTo("h2");
		assertThat(request.getHeader("Authorization")).isEqualTo("Basic am9objoxMjM=");
	}

	private interface GreetingService {

		@GetExchange("/greeting")
		String getGreeting();

	}


	static final class UserGroupConfigurer implements RestClientHttpServiceGroup.Configurer {

		@Nullable
		private String user;

		@Nullable
		private String password;

		private UserGroupConfigurer() {
		}

		public static UserGroupConfigurer auth() {
			return new UserGroupConfigurer();
		}

		public UserGroupConfigurer user(String user) {
			this.user = user;
			return this;
		}

		public UserGroupConfigurer password(String password) {
			this.password = password;
			return this;
		}

		@Override
		public void configure(HttpServiceGroup<RestClient.Builder> group) {
			Assert.state(this.user != null, "No user");
			Assert.state(this.password != null, "No password");
			group.configureClient(builder ->
					builder.defaultHeaders(headers -> headers.setBasicAuth(this.user, this.password)));
		}
	}
}
