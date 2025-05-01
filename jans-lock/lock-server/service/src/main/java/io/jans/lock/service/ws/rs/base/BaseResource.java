/*
 * Copyright [2024] [Janssen Project]
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.jans.lock.service.ws.rs.base;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.jans.lock.model.core.LockApiError;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.InternalServerErrorException;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;

public class BaseResource {

	private static final String MISSING_ATTRIBUTE_CODE = "OCA001";
	private static final String MISSING_ATTRIBUTE_MESSAGE = "A required attribute is missing.";

	@Context
	UriInfo uriInfo;

	@Context
	private HttpServletRequest httpRequest;

	@Context
	private HttpHeaders httpHeaders;

	private static Logger log = LoggerFactory.getLogger(BaseResource.class);

	public UriInfo getUriInfo() {
		return uriInfo;
	}

	public HttpServletRequest getHttpRequest() {
		return httpRequest;
	}

	public HttpHeaders getHttpHeaders() {
		return httpHeaders;
	}

	public static <T> void checkResourceNotNull(T resource, String objectName) {
		if (resource == null) {
			throw new NotFoundException(getNotFoundError(objectName));
		}
	}

	public static void checkNotNull(String attribute, String attributeName) {
		if (StringUtils.isBlank(attribute)) {
			throw new BadRequestException(getMissingAttributeError(attributeName));
		}
	}

	public static void checkNotNull(String[] attributes, String attributeName) {
		if (attributes == null || attributes.length <= 0) {
			throw new BadRequestException(getMissingAttributeError(attributeName));
		}
	}

	public static void checkNotNull(Map<String, String> attributeMap) {
		if (attributeMap.isEmpty()) {
			return;
		}

		Map<String, String> map = attributeMap.entrySet().stream()
				.filter(k -> (k.getValue() == null || StringUtils.isNotEmpty(k.getValue())))
				.collect(Collectors.toMap(Entry::getKey, Entry::getValue));

		log.debug(" map:{}", map);
		if (!map.isEmpty()) {
			throw new BadRequestException(getMissingAttributeError(map.keySet().toString()));
		}
	}

	public static <T> void checkNotEmpty(List<T> list, String attributeName) {
		if (list == null || list.isEmpty()) {
			throw new BadRequestException(getMissingAttributeError(attributeName));
		}
	}

	public static void checkNotEmpty(String attribute, String attributeName) {
		if (StringUtils.isEmpty(attribute)) {
			throw new BadRequestException(getMissingAttributeError(attributeName));
		}
	}

	public static void throwBadRequestException(String msg) {
		throw new BadRequestException(getBadRequestException(msg));
	}

	public static void throwBadRequestException(String msg, String description) {
		throw new BadRequestException(getBadRequestException(msg, description));
	}

	public static void throwBadRequestException(Object obj) {
		throw new BadRequestException(getBadRequestException(obj));
	}

	public static void throwInternalServerException(String msg) {
		throw new InternalServerErrorException(getInternalServerException(msg));
	}

	public static void throwInternalServerException(String msg, String description) {
		throw new InternalServerErrorException(getInternalServerException(msg, description));
	}

	public static void throwInternalServerException(String msg, Throwable throwable) {
		throwable = findRootError(throwable);
		if (throwable != null) {
			throw new InternalServerErrorException(getInternalServerException(msg, throwable.getMessage()));
		}
	}

	public static void throwInternalServerException(Throwable throwable) {
		throwable = findRootError(throwable);
		if (throwable != null) {
			throw new InternalServerErrorException(getInternalServerException(throwable.getMessage()));
		}
	}

	public static void throwNotFoundException(String msg) {
		throw new NotFoundException(getNotFoundError(msg));
	}

	public static void throwNotFoundException(String msg, String description) {
		throw new NotFoundException(getNotFoundError(msg, description));
	}

	protected static Response getMissingAttributeError(String attributeName) {
		LockApiError error = new LockApiError.ErrorBuilder().withCode(MISSING_ATTRIBUTE_CODE)
				.withMessage(MISSING_ATTRIBUTE_MESSAGE)
				.andDescription("The attribute " + attributeName + " is required for this operation").build();
		return Response.status(Response.Status.BAD_REQUEST).entity(error).build();
	}

	protected static Response getNotFoundError(String objectName) {
		LockApiError error = new LockApiError.ErrorBuilder()
				.withCode(String.valueOf(Response.Status.NOT_FOUND.getStatusCode()))
				.withMessage("The requested " + objectName + " doesn't exist").build();
		return Response.status(Response.Status.NOT_FOUND).entity(error).build();
	}

	protected static Response getNotFoundError(String msg, String description) {
		LockApiError error = new LockApiError.ErrorBuilder()
				.withCode(String.valueOf(Response.Status.NOT_FOUND.getStatusCode())).withMessage(msg)
				.andDescription(description).build();
		return Response.status(Response.Status.NOT_FOUND).entity(error).build();
	}

	protected static Response getNotAcceptableException(String msg) {
		LockApiError error = new LockApiError.ErrorBuilder()
				.withCode(String.valueOf(Response.Status.NOT_ACCEPTABLE.getStatusCode())).withMessage(msg).build();
		return Response.status(Response.Status.NOT_ACCEPTABLE).entity(error).build();
	}

	protected static Response getBadRequestException(String msg) {
		LockApiError error = new LockApiError.ErrorBuilder()
				.withCode(String.valueOf(Response.Status.BAD_REQUEST.getStatusCode())).withMessage(msg).build();
		return Response.status(Response.Status.BAD_REQUEST).entity(error).build();
	}

	protected static Response getBadRequestException(String msg, String description) {
		LockApiError error = new LockApiError.ErrorBuilder()
				.withCode(String.valueOf(Response.Status.BAD_REQUEST.getStatusCode())).withMessage(msg)
				.andDescription(description).build();
		return Response.status(Response.Status.BAD_REQUEST).entity(error).build();
	}

	protected static Response getBadRequestException(Object obj) {
		return Response.status(Response.Status.BAD_REQUEST).entity(obj).build();
	}

	protected static Response getInternalServerException(String msg) {
		LockApiError error = new LockApiError.ErrorBuilder()
				.withCode(String.valueOf(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode())).withMessage(msg)
				.build();
		return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(error).build();
	}

	protected static Response getInternalServerException(String msg, String description) {
		LockApiError error = new LockApiError.ErrorBuilder()
				.withCode(String.valueOf(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode())).withMessage(msg)
				.andDescription(description).build();
		return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(error).build();
	}

	public static Throwable findRootError(Throwable throwable) {
		if (throwable == null) {
			return throwable;
		}
		Throwable rootCause = throwable;
		while (rootCause.getCause() != null && rootCause.getCause() != rootCause) {
			rootCause = rootCause.getCause();
		}
		return rootCause;
	}

}
