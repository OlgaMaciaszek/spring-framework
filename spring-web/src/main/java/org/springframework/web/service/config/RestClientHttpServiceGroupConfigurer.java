package org.springframework.web.service.config;

import org.springframework.web.client.RestClient;
import org.springframework.web.service.registry.HttpServiceGroup;

/**
 * @author Olga Maciaszek-Sharma
 */
public interface RestClientHttpServiceGroupConfigurer extends HttpServiceGroup.Configurer<RestClient.Builder> {
}
