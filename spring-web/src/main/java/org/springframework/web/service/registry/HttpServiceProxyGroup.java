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

import java.util.Collections;
import java.util.Map;

/**
 * A group of HTTP service types and their proxies that share the same HTTP client setup.
 *
 * @author Rossen Stoyanchev
 * @since 7.0
 */
public final class HttpServiceProxyGroup {

	private final String baseUrl;

	private final String name;

	private final Map<Class<?>, Object> proxies;


	private HttpServiceProxyGroup(String baseUrl, String name, Map<Class<?>, Object> proxies) {
		this.baseUrl = baseUrl;
		this.name = name;
		this.proxies = Collections.unmodifiableMap(proxies);
	}


	public String baseUrl() {
		return this.baseUrl;
	}

	public String name() {
		return this.name;
	}

	public Map<Class<?>, Object> proxies() {
		return this.proxies;
	}


	public static HttpServiceProxyGroup create(HttpServiceGroup<?> group) {
		return new HttpServiceProxyGroup(group.baseUrl(), group.name(), group.createProxies());
	}


	@Override
	public boolean equals(Object other) {
		return (other != null && getClass() == other.getClass() &&
				this.name.equals(((HttpServiceProxyGroup) other).name));
	}

	@Override
	public int hashCode() {
		return this.name.hashCode();
	}

	@Override
	public String toString() {
		return "HttpServiceProxyGroup '" + this.name + "'";
	}

}
