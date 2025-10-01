/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.configapi.core.rest;

import static io.jans.as.model.util.Util.escapeLog;

import io.jans.configapi.core.interceptor.RequestAuditInterceptor;
import io.jans.configapi.core.interceptor.RequestInterceptor;
import io.jans.configapi.core.model.ApiError;
import io.jans.model.FilterOperator;
import io.jans.model.SearchRequest;
import io.jans.configapi.core.util.Util;
import io.jans.orm.model.SortOrder;

import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.InternalServerErrorException;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RequestAuditInterceptor
@RequestInterceptor
public class BaseResource {
   
    @Inject
    Util util;
    
    @Context
    UriInfo uriInfo;
    
    @Context
    private HttpServletRequest httpRequest;

    @Context
    private HttpHeaders httpHeaders;
    
    public UriInfo getUriInfo() {
        return uriInfo;
    }

    public HttpServletRequest getHttpRequest() {
        return httpRequest;
    }
    
    public HttpHeaders getHttpHeaders() {
        return httpHeaders;
    }    

    private static Logger log = LoggerFactory.getLogger(BaseResource.class);

    private static final String MISSING_ATTRIBUTE_CODE = "OCA001";
    private static final String MISSING_ATTRIBUTE_MESSAGE = "A required attribute is missing.";
    private static final String TOKEN_DELIMITER = ",";
    private static final String FIELD_VALUE_SEPARATOR = "=";
    private static final List<String> FIELD_VALUE_SEPARATOR_ARR = FilterOperator.getAllOperatorSign();

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

    public static void throwMissingAttributeError(String attributeName) {
        if (StringUtils.isNotEmpty(attributeName)) {
            throw new BadRequestException(getMissingAttributeError(attributeName));
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
    

    /**
     * @param attributeName
     * @return Response
     */
    protected static Response getMissingAttributeError(String attributeName) {
        ApiError error = new ApiError.ErrorBuilder().withCode(MISSING_ATTRIBUTE_CODE)
                .withMessage(MISSING_ATTRIBUTE_MESSAGE)
                .andDescription("The attribute " + attributeName + " is required for this operation").build();
        return Response.status(Response.Status.BAD_REQUEST).entity(error).build();
    }

    protected static Response getNotFoundError(String objectName) {
        ApiError error = new ApiError.ErrorBuilder().withCode(String.valueOf(Response.Status.NOT_FOUND.getStatusCode()))
                .withMessage("The requested " + objectName + " doesn't exist").build();
        return Response.status(Response.Status.NOT_FOUND).entity(error).build();
    }

    protected static Response getNotFoundError(String msg, String description) {
        ApiError error = new ApiError.ErrorBuilder()
                .withCode(String.valueOf(Response.Status.NOT_FOUND.getStatusCode())).withMessage(msg).andDescription(description)
                .build();
        return Response.status(Response.Status.NOT_FOUND).entity(error).build();
    }
    
    protected static Response getNotAcceptableException(String msg) {
        ApiError error = new ApiError.ErrorBuilder()
                .withCode(String.valueOf(Response.Status.NOT_ACCEPTABLE.getStatusCode())).withMessage(msg).build();
        return Response.status(Response.Status.NOT_ACCEPTABLE).entity(error).build();
    }

    protected static Response getBadRequestException(String msg) {
        ApiError error = new ApiError.ErrorBuilder()
                .withCode(String.valueOf(Response.Status.BAD_REQUEST.getStatusCode())).withMessage(msg).build();
        return Response.status(Response.Status.BAD_REQUEST).entity(error).build();
    }
    
    protected static Response getBadRequestException(String msg, String description) {
        ApiError error = new ApiError.ErrorBuilder()
                .withCode(String.valueOf(Response.Status.BAD_REQUEST.getStatusCode())).withMessage(msg).andDescription(description).build();
        return Response.status(Response.Status.BAD_REQUEST).entity(error).build();
    }

    protected static Response getBadRequestException(Object obj) {
        return Response.status(Response.Status.BAD_REQUEST).entity(obj).build();
    }

    protected static Response getInternalServerException(String msg) {
        ApiError error = new ApiError.ErrorBuilder()
                .withCode(String.valueOf(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode())).withMessage(msg)
                .build();
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(error).build();
    }
    
    protected static Response getInternalServerException(String msg, String description) {
        ApiError error = new ApiError.ErrorBuilder()
                .withCode(String.valueOf(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode())).withMessage(msg).andDescription(description)
                .build();
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(error).build();
    }

    protected SearchRequest createSearchRequest(String schemas, String filter, String sortBy, String sortOrder,
            Integer startIndex, Integer count, String attrsList, String excludedAttrsList, int maximumRecCount, String fieldValuePair, Class<?> entityClass) {
        if (log.isDebugEnabled()) {
            log.debug(
                    "Search Request params:: - schemas:{}, filter:{}, sortBy:{}, sortOrder:{}, startIndex:{}, count:{}, attrsList:{}, excludedAttrsList:{}, maximumRecCount:{}, fieldValuePair:{}, entityClass:{}",
                    escapeLog(schemas), escapeLog(filter), escapeLog(sortBy), escapeLog(sortOrder),
                    escapeLog(startIndex), escapeLog(count), escapeLog(attrsList), escapeLog(excludedAttrsList),
                    escapeLog(maximumRecCount), escapeLog(fieldValuePair), escapeLog(entityClass));
        }
        
        SearchRequest searchRequest = new SearchRequest();
        searchRequest.setEntityClass(entityClass);

        // Validation
        checkNotEmpty(schemas, "Schema");
        int maxCount = maximumRecCount;
        log.trace(" count:{}, maxCount:{}", count, maxCount);
        if (count > maxCount) {
            throwBadRequestException("Maximum number of results per page is " + maxCount);
        }

        count = count == null ? maxCount : count;
        log.trace(" count:{} ", count);
        // Per spec, a negative value SHALL be interpreted as "0" for count
        if (count < 0) {
            count = 0;
        }

        if (StringUtils.isEmpty(sortOrder) || !sortOrder.equals(SortOrder.DESCENDING.getValue())) {
            sortOrder = SortOrder.ASCENDING.getValue();
        }
        log.debug(
                " util.getTokens(filter,TOKEN_DELIMITER):{} , util.getFieldValueMap(searchRequest, fieldValuePair, TOKEN_DELIMITER, FIELD_VALUE_SEPARATOR)):{}, FIELD_VALUE_SEPARATOR_ARR:{}",
                util.getTokens(filter, TOKEN_DELIMITER),
                util.getFieldValueMap(entityClass, fieldValuePair, TOKEN_DELIMITER, FIELD_VALUE_SEPARATOR),
                FIELD_VALUE_SEPARATOR_ARR);
        searchRequest.setSchemas(schemas);
        searchRequest.setAttributes(attrsList);
        searchRequest.setExcludedAttributes(excludedAttrsList);
        searchRequest.setFilter(filter);
        searchRequest.setSortBy(sortBy);
        searchRequest.setSortOrder(sortOrder);
        searchRequest.setStartIndex(startIndex);
        searchRequest.setCount(count);
        searchRequest.setMaxCount(maximumRecCount);
        searchRequest.setFilterAssertionValue(util.getTokens(filter, TOKEN_DELIMITER));
        searchRequest.setFieldValueMap(
                (util.getFieldValueMap(entityClass, fieldValuePair, TOKEN_DELIMITER, FIELD_VALUE_SEPARATOR)));
        searchRequest.setFieldFilterData(
                (util.getFieldValueList(entityClass, fieldValuePair, TOKEN_DELIMITER, FIELD_VALUE_SEPARATOR_ARR)));
        return searchRequest;

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
