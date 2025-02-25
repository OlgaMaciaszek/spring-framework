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

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.regex.Pattern;

import org.jspecify.annotations.Nullable;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.core.type.filter.AssignableTypeFilter;
import org.springframework.core.type.filter.RegexPatternTypeFilter;
import org.springframework.core.type.filter.TypeFilter;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.invoker.HttpExchangeAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

/**
 * Base class for {@link HttpServiceGroup} implementations.
 *
 * @author Rossen Stoyanchev
 * @since 7.0
 * @param <G> the group type
 * @param <CB> the client builder type
 */
public abstract class AbstractHttpServiceGroup<G extends AbstractHttpServiceGroup<G, CB>, CB>
		implements HttpServiceGroup<G, CB> {

	private final String baseUrl;

	private final @Nullable String name;

	private final CB baseClientBuilder;

	private final ClassPathScanningCandidateComponentProvider componentProvider;

	private Consumer<HttpServiceProxyFactory.Builder> proxyFactoryConfigurer = builder -> {};

	private final Set<Class<?>> httpServiceTypes = new LinkedHashSet<>();

	private @Nullable Map<Class<?>, Object> proxyMap;


	protected AbstractHttpServiceGroup(String baseUrl, @Nullable String name, CB baseClientBuilder,
			ClassPathScanningCandidateComponentProvider componentProvider) {

		this.baseUrl = baseUrl;
		this.name = name;
		this.baseClientBuilder = baseClientBuilder;
		this.componentProvider = componentProvider;
	}


	@Override
	public String baseUrl() {
		return this.baseUrl;
	}

	@Override
	public @Nullable String name() {
		return this.name;
	}

	@Override
	public Set<Class<?>> httpServiceTypes() {
		return this.httpServiceTypes;
	}

	@Override
	public G addHttpServiceTypes(Class<?>... httpServiceTypes) {
		this.httpServiceTypes.addAll(Arrays.asList(httpServiceTypes));
		return self();
	}

	@Override
	public G detectHttpServiceTypes(Consumer<ScanSpec> scanConfigurer) {
		DefaultScanSpec scan = new DefaultScanSpec();
		scan.getIncludeFilters().forEach(this.componentProvider::addIncludeFilter);
		scan.getExcludeFilters().forEach(this.componentProvider::addExcludeFilter);

		for (String basePackage : scan.getBasePackages()) {
			for (BeanDefinition definition : this.componentProvider.findCandidateComponents(basePackage)) {
				String className = definition.getBeanClassName();
				if (className == null) {
					continue;
				}
				try {
					Class<?> clazz = ClassUtils.forName(className, getClass().getClassLoader());
					this.httpServiceTypes.add(clazz);
				}
				catch (ClassNotFoundException ex) {
					throw new RuntimeException("Failed to find '" + className + "'", ex);
				}
			}
		}

		return self();
	}

	@Override
	public G configureClient(Consumer<CB> configurer) {
		configurer.accept(this.baseClientBuilder);
		return self();
	}

	@Override
	public G configureProxyFactory(Consumer<HttpServiceProxyFactory.Builder> configurer) {
		this.proxyFactoryConfigurer = this.proxyFactoryConfigurer.andThen(configurer);
		return self();
	}

	@SuppressWarnings("unchecked")
	protected <S extends G> S self() {
		return (S) this;
	}

	@Override
	public void initProxies() {
		if (this.proxyMap != null) {
			return;
		}
		this.proxyMap = new HashMap<>(this.httpServiceTypes.size());
		HttpServiceProxyFactory factory = initProxyFactory();
		for (Class<?> type : this.httpServiceTypes) {
			this.proxyMap.put(type, factory.createClient(type));
		}
	}

	private HttpServiceProxyFactory initProxyFactory() {
		HttpExchangeAdapter adapter = createExchangeAdapter(this.baseClientBuilder);
		HttpServiceProxyFactory.Builder proxyFactoryBuilder = HttpServiceProxyFactory.builderFor(adapter);
		this.proxyFactoryConfigurer.accept(proxyFactoryBuilder);
		return proxyFactoryBuilder.build();
	}

	protected abstract HttpExchangeAdapter createExchangeAdapter(CB baseClientBuilder);

	@SuppressWarnings("unchecked")
	@Override
	public <T> T getProxy(Class<T> proxyType) {
		if(this.proxyMap == null) {
			initProxies();
		}
		Assert.state(this.proxyMap != null, "Proxies not initialized yet");
		T proxy = (T) this.proxyMap.get(proxyType);
		Assert.state(proxy != null, "No proxy of type [" + proxyType + "]");
		return proxy;
	}

	@Override
	public Map<Class<?>, Object> getProxies() {
		return (this.proxyMap != null ? this.proxyMap : Collections.emptyMap());
	}

	@Override
	public boolean equals(Object o) {
		if (!(o instanceof AbstractHttpServiceGroup<?, ?> that)) return false;
		return Objects.equals(baseUrl, that.baseUrl) && Objects.equals(name, that.name);
	}

	@Override
	public int hashCode() {
		return Objects.hash(baseUrl, name);
	}

	private static class DefaultScanSpec implements ScanSpec {

		private final List<String> basePackages = new ArrayList<>();

		private @Nullable List<Class<?>> assignableTypes;

		private @Nullable List<Class<? extends Annotation>> annotationTypes;

		private @Nullable List<String> expressions;

		private @Nullable List<TypeFilter> excludeFilters;

		@Override
		public ScanSpec basePackages(String... basePackages) {
			this.basePackages.addAll(Arrays.asList(basePackages));
			return this;
		}

		@Override
		public ScanSpec basePackages(Class<?>... basePackageClasses) {
			for (Class<?> type : basePackageClasses) {
				this.basePackages.add(type.getPackageName());
			}
			return this;
		}

		@Override
		public ScanSpec assignableTypes(Class<?>... assignableTypes) {
			this.assignableTypes = (this.assignableTypes != null ? this.assignableTypes : new ArrayList<>());
			this.assignableTypes.addAll(Arrays.asList(assignableTypes));
			return this;
		}

		@Override
		public ScanSpec annotation(Class<? extends Annotation> annotation) {
			this.annotationTypes = (this.annotationTypes != null ? this.annotationTypes : new ArrayList<>());
			this.annotationTypes.add(annotation);
			return this;
		}

		@Override
		public ScanSpec regex(String... expressions) {
			this.expressions = (this.expressions != null ? this.expressions : new ArrayList<>());
			this.expressions.addAll(Arrays.asList(expressions));
			return this;
		}

		@Override
		public ScanSpec excludeFilters(TypeFilter... excludeFilters) {
			this.excludeFilters = (this.excludeFilters != null ? this.excludeFilters : new ArrayList<>());
			this.excludeFilters.addAll(Arrays.asList(excludeFilters));
			return this;
		}

		List<String> getBasePackages() {
			Assert.notEmpty(this.basePackages, "No basePackage specified for HttpService scan");
			return this.basePackages;
		}

		List<TypeFilter> getIncludeFilters() {
			List<TypeFilter> typeFilters = new ArrayList<>();
			if (this.assignableTypes != null) {
				for (Class<?> assignableType : this.assignableTypes) {
					typeFilters.add(new AssignableTypeFilter(assignableType));
				}
			}
			if (this.annotationTypes != null) {
				for (Class<? extends Annotation> annotationType : this.annotationTypes) {
					typeFilters.add(new AnnotationTypeFilter(annotationType));
				}
			}
			if (this.expressions != null) {
				for (String expression : this.expressions) {
					typeFilters.add(new RegexPatternTypeFilter(Pattern.compile(expression)));
				}
			}
			if (typeFilters.isEmpty()) {
				typeFilters.add(new AnnotationTypeFilter(HttpExchange.class));
			}
			return typeFilters;
		}

		List<TypeFilter> getExcludeFilters() {
			return (this.excludeFilters != null ? this.excludeFilters : Collections.emptyList());
		}

	}

}
