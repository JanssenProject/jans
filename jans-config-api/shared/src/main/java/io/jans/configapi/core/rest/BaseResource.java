/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.configapi.core.rest;

import static io.jans.as.model.util.Util.escapeLog;

import io.jans.configapi.core.interceptor.RequestInterceptor;
import io.jans.configapi.core.model.ApiError;
import io.jans.configapi.core.model.SearchRequest;
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

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RequestInterceptor
public class BaseResource {
   
    @Inject
    Util util;

    private static Logger log = LoggerFactory.getLogger(BaseResource.class);

    public static final String MISSING_ATTRIBUTE_CODE = "OCA001";
    public static final String MISSING_ATTRIBUTE_MESSAGE = "A required attribute is missing.";
    public static final String TOKEN_DELIMITER = ",";

    public static <T> void checkResourceNotNull(T resource, String objectName) {
        if (resource == null) {
            throw new NotFoundException(getNotFoundError(objectName));
        }
    }

    public static void checkNotNull(String attribute, String attributeName) {
        if (attribute == null) {
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

    public static void thorwBadRequestException(String msg) {
        throw new BadRequestException(getBadRequestException(msg));
    }

    public static void thorwBadRequestException(Object obj) {
        throw new BadRequestException(getBadRequestException(obj));
    }

    public static void thorwInternalServerException(String msg) {
        throw new InternalServerErrorException(getInternalServerException(msg));
    }

    public static void thorwInternalServerException(Throwable throwable) {
        throwable = findRootError(throwable);
        if (throwable != null) {
            throw new InternalServerErrorException(getInternalServerException(throwable.getMessage()));
        }
    }

    /**
     * @param attributeName
     * @return
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

    protected static Response getBadRequestException(Object obj) {
        return Response.status(Response.Status.BAD_REQUEST).entity(obj).build();
    }

    protected static Response getInternalServerException(String msg) {
        ApiError error = new ApiError.ErrorBuilder()
                .withCode(String.valueOf(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode())).withMessage(msg)
                .build();
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(error).build();
    }

    protected SearchRequest createSearchRequest(String schemas, String filter, String sortBy, String sortOrder,
            Integer startIndex, Integer count, String attrsList, String excludedAttrsList, int maximumRecCount) {
        if (log.isDebugEnabled()) {
            log.debug(
                    "Search Request params:: - schemas:{}, filter:{}, sortBy:{}, sortOrder:{}, startIndex:{}, count:{}, attrsList:{}, excludedAttrsList:{}, maximumRecCount:{}",
                    escapeLog(schemas), escapeLog(filter), escapeLog(sortBy), escapeLog(sortOrder),
                    escapeLog(startIndex), escapeLog(count), escapeLog(attrsList), escapeLog(excludedAttrsList),
                    escapeLog(maximumRecCount));
        }
        SearchRequest searchRequest = new SearchRequest();

        // Validation
        checkNotEmpty(schemas, "Schema");
        int maxCount = maximumRecCount;
        log.debug(" count:{}, maxCount:{}", count, maxCount);
        if (count > maxCount) {
            thorwBadRequestException("Maximum number of results per page is " + maxCount);
        }

        count = count == null ? maxCount : count;
        log.debug(" count:{} ", count);
        // Per spec, a negative value SHALL be interpreted as "0" for count
        if (count < 0) {
            count = 0;
        }

        if (StringUtils.isEmpty(sortOrder) || !sortOrder.equals(SortOrder.DESCENDING.getValue())) {
            sortOrder = SortOrder.ASCENDING.getValue();
        }
        log.debug(" util.getTokens(filter,TOKEN_DELIMITER):{} ", util.getTokens(filter, TOKEN_DELIMITER));
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
