/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.configapi.rest.resource.auth;

import io.jans.configapi.rest.model.ApiError;
import io.jans.configapi.rest.model.SearchRequest;
import io.jans.configapi.util.ApiConstants;
import io.jans.configapi.configuration.ConfigurationFactory;
import io.jans.orm.model.SortOrder;

import javax.inject.Inject;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.core.Response;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
/**
 * @author Mougang T.Gasmyr
 *
 */
public class BaseResource {
    
    @Inject
    Logger log;

    @Inject
    ConfigurationFactory configurationFactory;    

    protected static final String READ_ACCESS = "config-api-read";
    protected static final String WRITE_ACCESS = "config-api-write";
    protected static final String DEFAULT_LIST_SIZE =  ApiConstants.DEFAULT_LIST_SIZE;
    //Pagination
    protected static final String DEFAULT_LIST_START_INDEX = ApiConstants.DEFAULT_LIST_START_INDEX;
    protected static final int DEFAULT_MAX_COUNT = ApiConstants.DEFAULT_MAX_COUNT;

    public static <T> void checkResourceNotNull(T resource, String objectName) {
        if (resource == null) {
            throw new NotFoundException(getNotFoundError(objectName));
        }
    }

    public static <T> void checkNotNull(String attribute, String attributeName) {
        if (attribute == null) {
            throw new BadRequestException(getMissingAttributeError(attributeName));
        }
    }

    public static <T> void checkNotEmpty(List<T> list, String attributeName) {
        if (list == null || list.isEmpty()) {
            throw new BadRequestException(getMissingAttributeError(attributeName));
        }
    }

    /**
     * @param attributeName
     * @return
     */
    protected static Response getMissingAttributeError(String attributeName) {
        ApiError error = new ApiError.ErrorBuilder().withCode(ApiConstants.MISSING_ATTRIBUTE_CODE)
                .withMessage(ApiConstants.MISSING_ATTRIBUTE_MESSAGE)
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

    protected Response prepareSearchRequest(String schemas, String filter, String sortBy, String sortOrder, Integer startIndex, Integer count,
            String attrsList, String excludedAttrsList, SearchRequest request) {
            log.debug("Search Request params:: - schemas:{}, filter:{}, sortBy:{}, sortOrder:{}, startIndex:{}, count:{}, attrsList:{}, excludedAttrsList:{}, request:{} ", schemas, filter, sortBy, sortOrder, startIndex, count, attrsList, excludedAttrsList, request);

            Response response = null;

            if (StringUtils.isNotEmpty(schemas)) {
                count = count == null ? getMaxCount() : count;
                //Per spec, a negative value SHALL be interpreted as "0" for count
                if (count < 0) {
                    count = 0;
                }

                if (count <= getMaxCount()) {
                    //SCIM searches are 1 indexed
                    startIndex = (startIndex == null || startIndex < 1) ? 1 : startIndex;

                    if (StringUtils.isEmpty(sortOrder) || !sortOrder.equals(SortOrder.DESCENDING.getValue())) {
                        sortOrder = SortOrder.ASCENDING.getValue();
                    }

                    request.setSchemas(schemas);
                    request.setAttributes(attrsList);
                    request.setExcludedAttributes(excludedAttrsList);
                    request.setFilter(filter);
                    request.setSortBy(sortBy);
                    request.setSortOrder(sortOrder);
                    request.setStartIndex(startIndex);
                    request.setCount(count);
                } else {
                    return Response.status(Response.Status.BAD_REQUEST).entity("Maximum number of results per page is " + getMaxCount()).build();
                }
            } else {
                return Response.status(Response.Status.BAD_REQUEST).entity("Schema(s) not supplied in Search Request").build();
            }
            return response;

        }
    
    protected int getMaxCount(){
        return (configurationFactory.getApiAppConfiguration().getMaxCount() !=0 ? configurationFactory.getApiAppConfiguration().getMaxCount() : DEFAULT_MAX_COUNT); 
    }

}
