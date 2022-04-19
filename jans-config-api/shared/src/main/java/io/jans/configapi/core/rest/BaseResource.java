/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.configapi.core.rest;

import static io.jans.as.model.util.Util.escapeLog;
import io.jans.configapi.core.model.ApiError;
import io.jans.configapi.core.model.SearchRequest;
import io.jans.orm.model.SortOrder;

import javax.inject.Inject;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.core.Response;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;

public class BaseResource {

    // Custom CODE
    public static final String MISSING_ATTRIBUTE_CODE = "OCA001";
    public static final String MISSING_ATTRIBUTE_MESSAGE = "A required attribute is missing.";

    @Inject
    Logger log;

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

        // SCIM searches are 1 indexed
        startIndex = (startIndex == null || startIndex < 1) ? 1 : startIndex;

        if (StringUtils.isEmpty(sortOrder) || !sortOrder.equals(SortOrder.DESCENDING.getValue())) {
            sortOrder = SortOrder.ASCENDING.getValue();
        }

        searchRequest.setSchemas(schemas);
        searchRequest.setAttributes(attrsList);
        searchRequest.setExcludedAttributes(excludedAttrsList);
        searchRequest.setFilter(filter);
        searchRequest.setSortBy(sortBy);
        searchRequest.setSortOrder(sortOrder);
        searchRequest.setStartIndex(startIndex);
        searchRequest.setCount(count);
        searchRequest.setMaxCount(maximumRecCount);

        return searchRequest;

    }

}
