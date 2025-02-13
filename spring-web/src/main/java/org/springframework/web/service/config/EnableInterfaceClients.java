package org.springframework.web.service.config;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.context.annotation.Import;

/**
 * Container annotation that aggregates several {@link EnableInterfaceClient} annotations.
 *
 * <p>Can be used natively, declaring several nested {@link EnableInterfaceClient} annotations.
 * Can also be used in conjunction with Java 8's support for repeatable annotations,
 * where {@link EnableInterfaceClient} can simply be declared several times on the same method,
 * implicitly generating this container annotation.
 *
 * @author Olga Maciaszek-Sharma
 * @since 7.0
 * @see EnableInterfaceClient
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Documented
@Import(HttpInterfaceClientsConfiguration.class)
public @interface EnableInterfaceClients {

	EnableInterfaceClient[] value() default {};
}

