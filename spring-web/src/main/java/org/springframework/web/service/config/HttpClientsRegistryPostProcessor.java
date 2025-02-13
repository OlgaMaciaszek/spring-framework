package org.springframework.web.service.config;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.jspecify.annotations.Nullable;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionReaderUtils;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.core.ResolvableType;
import org.springframework.core.annotation.MergedAnnotation;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.service.registry.HttpServiceGroup;
import org.springframework.web.service.registry.HttpServiceProxyGroup;
import org.springframework.web.service.registry.HttpServiceProxyRegistry;

/**
 * @author Olga Maciaszek-Sharma
 */
public class HttpClientsRegistryPostProcessor implements BeanDefinitionRegistryPostProcessor {

	@Override
	public void postProcessBeanDefinitionRegistry(@Nullable BeanDefinitionRegistry registry) throws BeansException {
		Assert.isInstanceOf(ListableBeanFactory.class, registry,
				"Registry must be an instance of " + ListableBeanFactory.class.getSimpleName());
		ListableBeanFactory beanFactory = (ListableBeanFactory) registry;

		// TODO: handle null
		HttpServiceProxyRegistry.Builder<?, ?> registryBuilder = beanFactory
				.getBean(HttpServiceProxyRegistry.Builder.class);


		// TODO
		Map<String, ?> serviceProxyGroupConfigurers =
				beanFactory.getBeansOfType(HttpServiceGroup.Configurer.class);

		Map<String, Set<MergedAnnotation<EnableInterfaceClient>>> annotationsMap = getAnnotations(beanFactory, registry);

		for (String key : annotationsMap.keySet()) {
			Set<MergedAnnotation<EnableInterfaceClient>> annotations = annotationsMap.get(key);
			for (MergedAnnotation<EnableInterfaceClient> annotation : annotations) {
				registryBuilder.addClient(annotation.getString(MergedAnnotation.VALUE),
						annotation.getString("name"),
						httpServiceConfigurer -> httpServiceConfigurer
								.addServiceTypes(annotation.getClassArray("httpServiceTypes"))
								.discoverServiceTypes(getBasePackages(annotation.getStringArray("basePackages"),
										annotation.getClassArray("basePackageClasses"), key)),
						// TODO
						clientBuilder -> {
						},
						proxyFactoryBuilder -> {
						});
			}
		}


		HttpServiceProxyRegistry interfaceClientRegistry = registryBuilder.build();

		registerBeanDefinition(registry, "httpInterfaceClientRegistry", HttpServiceProxyRegistry.class,
				interfaceClientRegistry);

		for (HttpServiceProxyGroup clientGroup : interfaceClientRegistry.getProxyGroups()) {
			Map<Class<?>, Object> proxies = clientGroup.proxies();
			for (Class<?> proxyClass : proxies.keySet()) {
				// TODO: * create better bean names from urls?
				String beanName = clientGroup.name() + proxyClass.getSimpleName();
				registerBeanDefinition(registry, beanName, proxyClass, proxies.get(proxyClass));
			}
		}
	}

	protected String[] getBasePackages(String[] basePackages,
			Class<?>[] basePackageClasses, String importingClassName) {
		Set<String> packages = new HashSet<>();
		for (String pkg : basePackages) {
			if (StringUtils.hasText(pkg)) {
				packages.add(pkg);
			}
		}

		for (Class<?> clazz : basePackageClasses) {
			packages.add(ClassUtils.getPackageName(clazz));
		}

		if (packages.isEmpty()) {
			packages.add(ClassUtils.getPackageName(importingClassName));
		}
		return packages.toArray(String[]::new);
	}

	private static void registerBeanDefinition(BeanDefinitionRegistry registry, String beanName, Class<?> beanClass,
			Object object) {
		BeanDefinition definition = BeanDefinitionBuilder
				.rootBeanDefinition(ResolvableType.forClass(beanClass), () -> object)
				.setAutowireMode(AbstractBeanDefinition.AUTOWIRE_BY_TYPE)
				.getBeanDefinition();
		BeanDefinitionHolder holder = new BeanDefinitionHolder(definition, beanName);
		BeanDefinitionReaderUtils.registerBeanDefinition(holder, registry);
	}

	private static Map<String, Set<MergedAnnotation<EnableInterfaceClient>>> getAnnotations(ListableBeanFactory beanFactory,
			BeanDefinitionRegistry registry) {
		String[] annotatedBeanNames = beanFactory.getBeanNamesForAnnotation(EnableInterfaceClient.class);
		Map<String, Set<MergedAnnotation<EnableInterfaceClient>>> annotations = new HashMap<>();
		for (String beanName : annotatedBeanNames) {
			BeanDefinition beanDefinition = registry.getBeanDefinition(beanName);
			Assert.isInstanceOf(AnnotatedBeanDefinition.class, beanDefinition);
			AnnotatedBeanDefinition annotatedBeanDefinition = (AnnotatedBeanDefinition) beanDefinition;
			AnnotationMetadata metadata = annotatedBeanDefinition.getMetadata();
			Set<MergedAnnotation<EnableInterfaceClient>> annotationSet = new HashSet<>();
			MergedAnnotation<EnableInterfaceClients> containerAnnotation = annotatedBeanDefinition.getMetadata()
					.getAnnotations()
					.get(EnableInterfaceClients.class);
			if (containerAnnotation.isPresent()) {
				Collections.addAll(annotationSet, containerAnnotation
						.getAnnotationArray(MergedAnnotation.VALUE, EnableInterfaceClient.class));
			}
			MergedAnnotation<EnableInterfaceClient> annotation = annotatedBeanDefinition.getMetadata()
					.getAnnotations()
					.get(EnableInterfaceClient.class);
			if (annotation.isPresent()) {
				annotationSet.add(annotation);
			}
			annotations.put(metadata.getClassName(), annotationSet);
		}
		return annotations;
	}
}


