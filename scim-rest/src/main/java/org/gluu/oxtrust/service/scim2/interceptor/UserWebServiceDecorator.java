/*
 * oxTrust is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2017, Gluu
 */
package org.gluu.oxtrust.service.scim2.interceptor;

import java.util.Collections;
import java.util.List;

import javax.annotation.Priority;
import javax.decorator.Decorator;
import javax.decorator.Delegate;
import javax.enterprise.inject.Any;
import javax.inject.Inject;
import javax.interceptor.Interceptor;
import javax.ws.rs.core.Response;

import org.apache.commons.lang.StringUtils;
import org.gluu.oxtrust.model.GluuCustomPerson;
import org.gluu.oxtrust.model.exception.SCIMException;
import org.gluu.oxtrust.model.scim2.ErrorScimType;
import org.gluu.oxtrust.model.scim2.SearchRequest;
import org.gluu.oxtrust.model.scim2.patch.PatchRequest;
import org.gluu.oxtrust.model.scim2.user.UserResource;
import org.gluu.oxtrust.model.scim2.util.ScimResourceUtil;
import org.gluu.oxtrust.service.IPersonService;
import org.gluu.oxtrust.ws.rs.scim2.BaseScimWebService;
import org.gluu.oxtrust.ws.rs.scim2.IUserWebService;
import io.jans.orm.exception.operation.DuplicateEntryException;
import org.slf4j.Logger;

/**
 * Aims at decorating SCIM user service methods. Currently applies validations via ResourceValidator class or other custom
 * validation logic
 *
 * Created by jgomer on 2017-09-01.
 */
@Priority(Interceptor.Priority.APPLICATION)
@Decorator
public class UserWebServiceDecorator extends BaseScimWebService implements IUserWebService {

    @Inject
    private Logger log;

    @Inject @Delegate @Any
    private IUserWebService service;

    @Inject
    private IPersonService personService;

    private void checkUidExistence(String uid) throws DuplicateEntryException{
        if (personService.getPersonByUid(uid) != null)
            throw new DuplicateEntryException("Duplicate UID value: " + uid);
    }

    private void checkUidExistence(String uid, String id) throws DuplicateEntryException{

        // Validate if there is an attempt to supply a userName already in use by a user other than current
        List<GluuCustomPerson> list=null;
        try{
            list=personService.findPersonsByUids(Collections.singletonList(uid), new String[]{"inum"});
        }
        catch (Exception e){
            log.error(e.getMessage(), e);
        }
        if (list!=null){
            for (GluuCustomPerson p : list)
                if (!p.getInum().equals(id))
                    throw new DuplicateEntryException("Duplicate UID value: " + uid);
        }

    }

    public Response createUser(UserResource user, String attrsList, String excludedAttrsList) {

        Response response;
        try {
            executeDefaultValidation(user);
            checkUidExistence(user.getUserName());

            assignMetaInformation(user);
            ScimResourceUtil.adjustPrimarySubAttributes(user);
            //Proceed with actual implementation of createUser method
            response = service.createUser(user, attrsList, excludedAttrsList);
        }
        catch (DuplicateEntryException e){
            log.error(e.getMessage());
            response=getErrorResponse(Response.Status.CONFLICT, ErrorScimType.UNIQUENESS, e.getMessage());
        }
        catch (SCIMException e){
            log.error("Validation check at createUser returned: {}", e.getMessage());
            response = getErrorResponse(Response.Status.BAD_REQUEST, ErrorScimType.INVALID_VALUE, e.getMessage());
        }
        return response;

    }

    public Response getUserById(String id, String attrsList, String excludedAttrsList){

        Response response=validateExistenceOfUser(id);
        if (response==null)
            //Proceed with actual implementation of getUserById method
            response= service.getUserById(id, attrsList, excludedAttrsList);

        return response;

    }

    public Response updateUser(UserResource user, String id, String attrsList, String excludedAttrsList) {

        Response response;
        try{
            //Check if the ids match in case the user coming has one
            if (user.getId()!=null && !user.getId().equals(id))
                throw new SCIMException("Parameter id does not match with id attribute of User");

            response=validateExistenceOfUser(id);
            if (response==null) {

                executeValidation(user, true);
                if (StringUtils.isNotEmpty(user.getUserName()))
                    checkUidExistence(user.getUserName(), id);

                ScimResourceUtil.adjustPrimarySubAttributes(user);
                //Proceed with actual implementation of updateUser method
                response = service.updateUser(user, id, attrsList, excludedAttrsList);
            }
        }
        catch (DuplicateEntryException e){
            log.error(e.getMessage());
            response=getErrorResponse(Response.Status.CONFLICT, ErrorScimType.UNIQUENESS, e.getMessage());
        }
        catch (SCIMException e){
            log.error("Validation check at updateUser returned: {}", e.getMessage());
            response = getErrorResponse(Response.Status.BAD_REQUEST, ErrorScimType.INVALID_VALUE, e.getMessage());
        }
        return response;

    }

    public Response deleteUser(String id){

        Response response=validateExistenceOfUser(id);
        if (response==null)
            //Proceed with actual implementation of deleteUser method
            response= service.deleteUser(id);

        return response;

    }

    public Response searchUsers(String filter, Integer startIndex, Integer count, String sortBy, String sortOrder,
                                String attrsList, String excludedAttrsList){

        SearchRequest searchReq=new SearchRequest();
        Response response=prepareSearchRequest(searchReq.getSchemas(), filter, sortBy, sortOrder, startIndex, count,
                                attrsList, excludedAttrsList, searchReq);

        if (response==null) {
            response = service.searchUsers(searchReq.getFilter(), searchReq.getStartIndex(), searchReq.getCount(),
                        searchReq.getSortBy(), searchReq.getSortOrder(), searchReq.getAttributesStr(), searchReq.getExcludedAttributesStr());
        }
        return response;

    }

    public Response searchUsersPost(SearchRequest searchRequest){

        SearchRequest searchReq=new SearchRequest();
        Response response=prepareSearchRequest(searchRequest.getSchemas(), searchRequest.getFilter(), searchRequest.getSortBy(),
                            searchRequest.getSortOrder(), searchRequest.getStartIndex(), searchRequest.getCount(),
                            searchRequest.getAttributesStr(), searchRequest.getExcludedAttributesStr(), searchReq);

        if (response==null) {
            response = service.searchUsersPost(searchReq);
        }
        return response;

    }

    public Response patchUser(PatchRequest request, String id, String attrsList, String excludedAttrsList){

        Response response=inspectPatchRequest(request, UserResource.class);
        if (response==null) {
            response=validateExistenceOfUser(id);

            if (response==null)
                response = service.patchUser(request, id, attrsList, excludedAttrsList);
        }
        return response;

    }

}
