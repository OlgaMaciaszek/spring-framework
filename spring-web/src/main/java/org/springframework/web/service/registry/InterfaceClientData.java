package org.springframework.web.service.registry;

/**
 * @author Olga Maciaszek-Sharma
 */
public record InterfaceClientData(String value, String name, String[] basePackages,
								  Class<?>[] basePackageClasses,
								  Class<?>[] httpServiceTypes,
								  String importingClassName) {
}