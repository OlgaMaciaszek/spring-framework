package org.springframework.web.client.support;

import java.net.URI;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.util.Assert;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.service.invoker.HttpExchangeAdapter;
import org.springframework.web.service.invoker.HttpRequestValues;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * An {@link HttpExchangeAdapter} that enables an {@link HttpServiceProxyFactory} to use
 * {@link RestTemplate} for request execution.
 *
 * Use static factory methods in this class to create an {@link HttpServiceProxyFactory}
 * configured with a given {@link RestTemplate}.
 *
 * @author Olga Maciaszek-Sharma
 * @since 6.1
 */
public class RestTemplateAdapter implements HttpExchangeAdapter {

	private final RestTemplate restTemplate;

	// Private constructor; use static factory methods to instantiate
	private RestTemplateAdapter(RestTemplate restTemplate) {
		this.restTemplate = restTemplate;
	}

	@Override
	public void exchange(HttpRequestValues requestValues) {
		restTemplate.exchange(newRequest(requestValues), Void.class);
	}

	@Override
	public HttpHeaders exchangeForHeaders(HttpRequestValues requestValues) {
		// TODO: test and handle return type
		return restTemplate.exchange(newRequest(requestValues), Void.class).getHeaders();
	}

	@Override
	public <T> T exchangeForBody(HttpRequestValues requestValues, ParameterizedTypeReference<T> bodyType) {
		return restTemplate.exchange(newRequest(requestValues), bodyType).getBody();
	}

	@Override
	public ResponseEntity<Void> exchangeForBodilessEntity(HttpRequestValues requestValues) {
		return restTemplate.exchange(newRequest(requestValues), Void.class);
	}

	@Override
	public <T> ResponseEntity<T> exchangeForEntity(HttpRequestValues requestValues,
			ParameterizedTypeReference<T> bodyType) {
		throw new UnsupportedOperationException("Please, implement me.");
	}

	private RequestEntity<?> newRequest(HttpRequestValues requestValues) {
		URI uri;
		if (requestValues.getUri() != null) {
			uri = requestValues.getUri();
		}
		else if (requestValues.getUriTemplate() != null) {
			uri = UriComponentsBuilder.fromUriString(requestValues.getUriTemplate())
				.build(requestValues.getUriVariables());
		}
		else {
			throw new IllegalStateException("Neither full URL nor URI template");
		}

		HttpMethod httpMethod = requestValues.getHttpMethod();
		Assert.notNull(httpMethod, "HttpMethod is required");

		RequestEntity.BodyBuilder builder = RequestEntity.method(httpMethod, uri)
			.headers(requestValues.getHeaders())
			// TODO: handle attributes
			// TODO: test and fix
			.headers(new HttpHeaders(requestValues.getCookies()));

		if (requestValues.getBodyValue() != null) {
			return builder.body(requestValues.getBodyValue());
		}
		return builder.build();
	}

	/**
	 * Create a {@link RestTemplateAdapter} for the given {@link RestTemplate} instance.
	 * @param restTemplate the {@link RestTemplate} to use
	 * @return the created adapter instance
	 */
	public static RestTemplateAdapter forTemplate(RestTemplate restTemplate) {
		return new RestTemplateAdapter(restTemplate);
	}

}
