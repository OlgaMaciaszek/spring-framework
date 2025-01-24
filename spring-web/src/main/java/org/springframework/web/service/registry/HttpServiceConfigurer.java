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

import org.springframework.core.type.filter.TypeFilter;

/**
 * Helps with registration of HTTP service types.
 *
 * @author Rossen Stoyanchev
 * @since 7.0
 */
public interface HttpServiceConfigurer {

	HttpServiceConfigurer addServiceTypes(Class<?>... httpServiceTypes);

	HttpServiceConfigurer discoverServiceTypes(
			String basePackage, List<TypeFilter> includeFilters, List<TypeFilter> excludeFilters);

	default HttpServiceConfigurer discoverServiceTypes(
			Class<?> basePackageClass, List<TypeFilter> includeFilters, List<TypeFilter> excludeFilters) {

		return discoverServiceTypes(basePackageClass.getName(), includeFilters, excludeFilters);
	}

	HttpServiceConfigurer discoverServiceTypes(
			String[] basePackages, List<TypeFilter> includeFilters, List<TypeFilter> excludeFilters);

	HttpServiceConfigurer discoverServiceTypes(
			Class<?>[] basePackages, List<TypeFilter> includeFilters, List<TypeFilter> excludeFilters);

}
