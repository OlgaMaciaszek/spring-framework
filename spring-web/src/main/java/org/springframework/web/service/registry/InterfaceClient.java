/*
 * Copyright 2012-2025 the original author or authors.
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

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Registers an HTTP service client along with associated interface clients.
 * Scans the listed packages for {@link @HttpExchange}-annotated interfaces
 * to add or adds the directly provided interfaces to the client.
 *
 * @author Olga Maciaszek-Sharma
 * @since 7.0
 * TODO: * should the name be general like the current one or `EnableHttpInterfaceClient`?
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Documented
@Repeatable(InterfaceClients.class)
public @interface InterfaceClient {

	/**
	 * The {@code baseUrl}  of the host or service the client communicates with.
	 * @return An absolute URL or resolvable serviceId
	 */
	String value();


	/**
	 * Name of the client. If not provided, resolved from url host value.
	 * @return client name
	 */
	String name() default "";


	/**
	 * Base packages to scan for annotated components.
	 * Use {@link #basePackageClasses()} for a type-safe alternative to String-based
	 * package names.
	 * @return the array of 'basePackages'
	 */
	String[] basePackages() default {};

	/**
	 * Type-safe alternative to {@link #basePackages()} for specifying the packages to
	 * scan for annotated components. The package of each class specified will be scanned.
	 * <p>
	 * Consider creating a special no-op marker class or interface in each package that
	 * serves no purpose other than being referenced by this attribute.
	 * @return the array of 'basePackageClasses'
	 */
	Class<?>[] basePackageClasses() default {};

	/**
	 * List of interface types to instantiate for the client. If not empty, disables classpath
	 * scanning.
	 * @return an array of {@link org.springframework.web.service.annotation.HttpExchange} classes
	 */
	Class<?>[] httpServiceTypes() default {};
}
