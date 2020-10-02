/*
 * oxTrust is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2017, Gluu
 */
package org.gluu.oxtrust.service.scim2.interceptor;

import javax.annotation.Priority;
import javax.decorator.Decorator;
import javax.decorator.Delegate;
import javax.enterprise.inject.Any;
import javax.inject.Inject;
import javax.interceptor.Interceptor;
import javax.ws.rs.core.Response;

import org.apache.commons.lang.StringUtils;
import org.gluu.oxtrust.model.exception.SCIMException;
import org.gluu.oxtrust.model.fido.GluuCustomFidoDevice;
import org.gluu.oxtrust.model.scim2.ErrorScimType;
import org.gluu.oxtrust.model.scim2.SearchRequest;
import org.gluu.oxtrust.model.scim2.fido.FidoDeviceResource;
import org.gluu.oxtrust.service.IFidoDeviceService;
import org.gluu.oxtrust.ws.rs.scim2.BaseScimWebService;
import org.gluu.oxtrust.ws.rs.scim2.IFidoDeviceWebService;
import org.slf4j.Logger;

/**
 * Aims at decorating SCIM fido service methods. Currently applies validations via ResourceValidator class or other custom
 * validation logic
 *
 * Created by jgomer on 2017-10-09.
 */
@Priority(Interceptor.Priority.APPLICATION)
@Decorator
public abstract class FidoDeviceWebServiceDecorator extends BaseScimWebService implements IFidoDeviceWebService {

    @Inject
    private Logger log;

    @Inject @Delegate @Any
    IFidoDeviceWebService service;

    @Inject
	private IFidoDeviceService fidoDeviceService;

    private Response validateExistenceOfDevice(String userId, String id) {
        //userId can be null here
        Response response=null;

        GluuCustomFidoDevice device = StringUtils.isEmpty(id) ? null : fidoDeviceService.getGluuCustomFidoDeviceById(userId, id);
        if (device == null) {
            log.info("Device with id {} not found", id);
            response = getErrorResponse(Response.Status.NOT_FOUND, ErrorScimType.INVALID_VALUE, "Resource " + id + " not found");
        }
        return response;

    }

    public Response getDeviceById(String id, String userId, String attrsList, String excludedAttrsList){
        Response response=validateExistenceOfDevice(userId, id);
        if (response==null)
            response=service.getDeviceById(id, userId, attrsList, excludedAttrsList);
        return response;

    }

    public Response updateDevice(FidoDeviceResource fidoDevice, String id, String attrsList, String excludedAttrsList){

        Response response;
        try {
            //remove externalId, no place to store it in LDAP
            fidoDevice.setExternalId(null);

            if (fidoDevice.getId()!=null && !fidoDevice.getId().equals(id))
                throw new SCIMException("Parameter id does not match id attribute of Device");

            response=validateExistenceOfDevice(fidoDevice.getUserId(), id);

            if (response==null) {
                executeValidation(fidoDevice, true);
                response = service.updateDevice(fidoDevice, id, attrsList, excludedAttrsList);
            }
        }
        catch (SCIMException e){
            log.error("Validation check at updateDevice returned: {}", e.getMessage());
            response = getErrorResponse(Response.Status.BAD_REQUEST, ErrorScimType.INVALID_VALUE, e.getMessage());
        }
        return response;

    }

    public Response deleteDevice(String id) {

        Response response=validateExistenceOfDevice(null, id);
        if (response==null)
            response=service.deleteDevice(id);
        return response;

    }

    public Response searchDevices(String userId, String filter, Integer startIndex, Integer count, String sortBy,
                                  String sortOrder, String attrsList, String excludedAttrsList) {

        SearchRequest searchReq=new SearchRequest();
        Response response=prepareSearchRequest(searchReq.getSchemas(), filter, sortBy, sortOrder, startIndex, count,
                                attrsList, excludedAttrsList, searchReq);

        if (response == null) {
            response = validateExistenceOfUser(userId);
            if (response == null) {
                response = service.searchDevices(userId, searchReq.getFilter(), searchReq.getStartIndex(), searchReq.getCount(),
                        searchReq.getSortBy(), searchReq.getSortOrder(), searchReq.getAttributesStr(), searchReq.getExcludedAttributesStr());
            }
        }
        return response;

    }

    public Response searchDevicesPost(SearchRequest searchRequest, String userId) {

        SearchRequest searchReq = new SearchRequest();
        Response response = prepareSearchRequest(searchRequest.getSchemas(), searchRequest.getFilter(), searchRequest.getSortBy(),
                                searchRequest.getSortOrder(), searchRequest.getStartIndex(), searchRequest.getCount(),
                                searchRequest.getAttributesStr(), searchRequest.getExcludedAttributesStr(), searchReq);

        if (response==null) {
            response = validateExistenceOfUser(userId);
            if (response == null) {
                response = service.searchDevicesPost(searchReq, userId);
            }
        }
        return response;
    }

}
