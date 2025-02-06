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
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import org.jspecify.annotations.Nullable;

import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.annotation.MergedAnnotation;
import org.springframework.core.env.Environment;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.CollectionUtils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

/**
 * Base class for {@link HttpServiceProxyRegistry} implementations.
 *
 * @author Rossen Stoyanchev
 * @since 7.0
 */
public abstract class AbstractHttpServiceProxyRegistry<CB> implements HttpServiceProxyRegistry {

	private final Set<HttpServiceGroup<?>> proxyGroups;

	private final Map<String, HttpServiceGroup<?>> proxyGroupLookup = new LinkedHashMap<>();

	private final MultiValueMap<Class<?>, Object> proxyTypeLookup = new LinkedMultiValueMap<>();


	protected AbstractHttpServiceProxyRegistry(Set<HttpServiceGroup<CB>> proxyGroups) {
		this.proxyGroups = Collections.unmodifiableSet(proxyGroups);

		proxyGroups.forEach(group -> {
			this.proxyGroupLookup.put(group.name(), group);
		});
	}

	@SuppressWarnings("unchecked")
	@Override
	public <S> @Nullable S getClient(Class<S> httpServiceType) {
		List<Object> proxies = this.proxyTypeLookup
				.computeIfAbsent(httpServiceType, this::getProxies);
		if (CollectionUtils.isEmpty(proxies)) {
			return null;
		}
		Assert.isTrue(proxies.size() == 1, () -> "More than one proxy for serviceType=" + httpServiceType);
		return (S) proxies.get(0);
	}

	@SuppressWarnings("unchecked")
	@Override
	public <S> @Nullable S getClient(String name, Class<S> httpServiceType) {
		HttpServiceGroup<?> group = this.proxyGroupLookup.get(name);
		if (group == null) {
			return null;
		}
		return (S) group.proxies().get(httpServiceType);
	}

	@Override
	public Set<HttpServiceGroup<?>> getProxyGroups() {
		return this.proxyGroups;
	}


	private <S> List<Object> getProxies(Class<S> httpServiceType) {
		return getProxyGroups().stream()
				.map(proxyGroup -> proxyGroup.proxies().entrySet())
				.flatMap(Collection::stream)
				.filter(entry -> entry.getKey().equals(httpServiceType))
				.map(Map.Entry::getValue)
				.toList();
	}


	/**
	 * Base class for {@link HttpServiceProxyRegistry.Builder} implementations.
	 * @param <B> the type of builder
	 * @param <CB> the client builder type
	 */
	protected abstract static class AbstractBuilder<B extends AbstractBuilder<B, CB>, CB> implements Builder<B, CB> {

		private final Set<HttpServiceGroup<CB>> groups = new HashSet<>();

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
				// Do not ignore interfaces while scanning
				this.componentProvider = new ClassPathScanningCandidateComponentProvider(false, this.environment) {
					@Override
					protected boolean isCandidateComponent(AnnotatedBeanDefinition beanDefinition) {
						AnnotationMetadata metadata = beanDefinition.getMetadata();
						return metadata.isIndependent() && !metadata.isAnnotation();
					}
				};
				this.componentProvider.setResourceLoader(this.resourceLoader);
			}
			return this.componentProvider;
		}

		@Override
		public Builder<B, CB> addClient(String baseUrl, @Nullable String name,
				Consumer<HttpServiceConfigurer> httpServiceConfigurerConsumer,
				Consumer<CB> clientBuilderConsumer,
				Consumer<HttpServiceProxyFactory.Builder> proxyFactoryBuilderConsumer) {

			String actualName = (name != null ? name : baseUrl);

			AbstractHttpServiceGroup<CB> group = createGroup(baseUrl, actualName);

			// Avoid failing silently if the user adds two groups under same name
			if(this.groups.contains(group)) {
				throw new IllegalArgumentException("Can only create one group with a given name.");
			}
			this.groups.add(group);

			group.configureHttpServices(httpServiceConfigurerConsumer);
			group.configureProxyFactory(proxyFactoryBuilderConsumer);
			group.configureClient(clientBuilderConsumer);

			return self();
		}

		protected abstract AbstractHttpServiceGroup<CB> createGroup(String baseUrl, String name);

		@Override
		public Builder<B, CB> addClient(InterfaceClientData interfaceClientData,
				Consumer<CB> clientBuilderConsumer) {

			if (interfaceClientData.httpServiceTypes().length != 0) {
				addClient(interfaceClientData.value(), interfaceClientData.name(),
						httpServiceConfigurer -> httpServiceConfigurer
								.addServiceTypes(interfaceClientData.httpServiceTypes()),
						clientBuilderConsumer, proxyFactoryBuilder -> {
						});
			}
			else {
				addClient(interfaceClientData.value(), interfaceClientData.name(),
						httpServiceConfigurer -> httpServiceConfigurer
								.discoverServiceTypes(getBasePackages(interfaceClientData)),
						clientBuilderConsumer, proxyFactoryBuilder -> {
						});
			}

			return this;
		}

		@Override
		public Builder<B, CB> apply(HttpServiceGroup.Configurer<CB> configurer) {
			this.groups.forEach(configurer::configure);
			return self();
		}

		@Override
		public HttpServiceProxyRegistry build() {
			return initRegistry(this.groups);
		}

		protected abstract HttpServiceProxyRegistry initRegistry(Set<HttpServiceGroup<CB>> proxyGroups);

		@SuppressWarnings("unchecked")
		protected <T extends B> T self() {
			return (T) this;
		}

		// TODO: move out of the builder?
		@Override
		public Set<InterfaceClientData> discoverClients(List<String> basePackages) {
			Set<BeanDefinition> annotationConfigClasses = discoverAnnotatedConfigurationClasses(basePackages);
			Set<InterfaceClientData> interfaceClientData = new HashSet<>();
			for (BeanDefinition annotationConfigClass : annotationConfigClasses) {
				if (annotationConfigClass instanceof AnnotatedBeanDefinition beanDefinition) {
					AnnotationMetadata annotatedBeanMetadata = beanDefinition.getMetadata();
					MergedAnnotation<? extends Annotation> annotation = annotatedBeanMetadata.getAnnotations()
							.get(InterfaceClient.class);
					interfaceClientData.add(new InterfaceClientData(annotation.getString(MergedAnnotation.VALUE),
							annotation.getString("name"),
							annotation.getStringArray("basePackages"),
							annotation.getClassArray("basePackageClasses"),
							annotation.getClassArray("httpServiceTypes"),
							annotatedBeanMetadata.getClassName()));
				}
			}
			return interfaceClientData;
		}

		private Set<BeanDefinition> discoverAnnotatedConfigurationClasses(List<String> basePackages) {
			Set<BeanDefinition> annotationConfigClasses = new HashSet<>();
			Environment environment = this.environment != null ? this.environment : new StandardEnvironment();
			ClassPathScanningCandidateComponentProvider componentProvider =
					new ClassPathScanningCandidateComponentProvider(false, environment);
			componentProvider.setResourceLoader(this.resourceLoader);
			componentProvider.setResourceLoader(this.resourceLoader);
			componentProvider.addIncludeFilter(new AnnotationTypeFilter(InterfaceClient.class));
			for (String basePackage : basePackages) {
				annotationConfigClasses.addAll(componentProvider.findCandidateComponents(basePackage));
			}
			return annotationConfigClasses;
		}

		protected String[] getBasePackages(InterfaceClientData clientData) {
			Set<String> basePackages = new HashSet<>();
			for (String pkg : clientData.basePackages()) {
				if (StringUtils.hasText(pkg)) {
					basePackages.add(pkg);
				}
			}
			for (Class<?> clazz : clientData.basePackageClasses()) {
				basePackages.add(ClassUtils.getPackageName(clazz));
			}

			if (basePackages.isEmpty()) {
				basePackages.add(ClassUtils.getPackageName(clientData.importingClassName()));
			}
			return basePackages.toArray(String[]::new);
		}
	}

}
