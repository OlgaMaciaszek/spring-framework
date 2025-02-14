package org.springframework.web.service.config;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Role;

/**
 * @author Olga Maciaszek-Sharma
 */
@Configuration
public class HttpInterfaceClientsConfiguration {

	@Bean
	@Role(BeanDefinition.ROLE_INFRASTRUCTURE)
	static HttpClientsRegistryPostProcessor httpInterfaceClientsRegistryPostProcessor() {
		return new HttpClientsRegistryPostProcessor();
	}

}
