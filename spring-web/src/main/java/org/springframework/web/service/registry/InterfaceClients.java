package org.springframework.web.service.registry;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Container annotation that aggregates several {@link InterfaceClient} annotations.
 *
 * <p>Can be used natively, declaring several nested {@link InterfaceClient} annotations.
 * Can also be used in conjunction with Java 8's support for repeatable annotations,
 * where {@link InterfaceClient} can simply be declared several times on the same method,
 * implicitly generating this container annotation.
 *
 * @author Olga Maciaszek-Sharma
 * @since 7.0
 * @see InterfaceClient
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Documented
public @interface InterfaceClients {

	InterfaceClient[] value();
}

