/*
 * Copyright 2002-2023 the original author or authors.
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

package org.springframework.web.service.invoker;

import org.springframework.core.MethodParameter;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

/**
 * {@link HttpServiceArgumentResolver} for {@link MultipartFile} arguments.
 *
 * @author Olga Maciaszek-Sharma
 */
public class MultipartFileArgumentResolver extends AbstractNamedValueArgumentResolver {

	private static final String MULTIPART_FILE_LABEL = "multipart file";

	@Override
	protected NamedValueInfo createNamedValueInfo(MethodParameter parameter) {
		if (!parameter.getParameterType().equals(MultipartFile.class)) {
			return null;
		}
		// Assuming the annotation might be there even if not required
		RequestPart annotation = parameter.getParameterAnnotation(RequestPart.class);
		if (annotation != null) {
			return new NamedValueInfo(annotation.name(), annotation.required(),
					null, MULTIPART_FILE_LABEL, true);
		}
		String parameterName = parameter.getParameterName();
		return new NamedValueInfo(parameterName != null ? parameterName : "", true,
				null, MULTIPART_FILE_LABEL, true);

	}

	@Override
	protected void addRequestValue(String name, Object value, MethodParameter parameter,
			HttpRequestValues.Builder requestValues) {
		Assert.isInstanceOf(MultipartFile.class, value,
				"The value has to be of type 'MultipartFile'");
		Assert.isInstanceOf(MultipartFile.class, value,
				"The value has to be of type 'MultipartFile'");

		MultipartFile file = (MultipartFile) value;
		requestValues.addMultipartFile(name, toHttpEntity(name, file));
	}

	private HttpEntity<Resource> toHttpEntity(String name, MultipartFile file) {
		HttpHeaders headers = new HttpHeaders();
		if (file.getOriginalFilename() != null) {
			headers.setContentDispositionFormData(name, file.getOriginalFilename());
		}
		if (file.getContentType() != null) {
			headers.setContentType(MediaType.parseMediaType(file.getContentType()));
		}
		return new HttpEntity<>(file.getResource(), headers);
	}

}
