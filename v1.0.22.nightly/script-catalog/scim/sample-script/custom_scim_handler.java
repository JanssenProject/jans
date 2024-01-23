/* Copyright (c) 2022, Janssen Project
*
* Author: Gluu
*    1. Modifying Search Results
*    2. Segmenting the user base
*    3. Allow/Deny resource operations
*    4. Allow/Deny searches
*/


import io.jans.model.custom.script.type.scim.ScimType;
import io.jans.scim.ws.rs.scim2.BaseScimWebService;
import io.jans.service.custom.script.CustomScriptManager;
import io.jans.model.custom.script.model.CustomScript;
import io.jans.model.SimpleCustomProperty;
import io.jans.scim.model.scim.ScimCustomPerson;
import io.jans.scim.model.GluuGroup;
import io.jans.orm.model.PagedResult;
import io.jans.scim.service.external.OperationContext;
import io.jans.orm.model.base.Entry;
import jakarta.ws.rs.core.Response;
import io.jans.scim.model.scim2.SearchRequest;
import jakarta.ws.rs.core.MultivaluedMap;

import org.json.JSONObject;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ScimEventHandler implements ScimType {

    private static final Logger log = LoggerFactory.getLogger(ScimEventHandler.class);
    private static final Logger scriptLogger = LoggerFactory.getLogger(CustomScriptManager.class);
    private String custom_header;
    private JSONObject access_map;

    @Override
    public boolean init(Map<String, SimpleCustomProperty> configurationAttributes) {
        custom_header = configurationAttributes.get("custom_header").getValue2();
        String access_map_json = configurationAttributes.get("access_map").getValue2();    
        access_map = new JSONObject(access_map_json);
        log.info("Custom Java ScimEventHandler (init)");
        scriptLogger.info("Custom Java ScimEventHandler (init): Initializing ...");
        scriptLogger.info("Custom Java ScimEventHandler (init): Initialized successfully");        
        return true;
    }

    @Override
    public boolean init(CustomScript customScript, Map<String, SimpleCustomProperty> configurationAttributes) {
        log.info("Custom Java ScimEventHandler (init)");
        scriptLogger.info("Custom Java ScimEventHandler (init): Initializing ...");
        scriptLogger.info("Custom Java ScimEventHandler (init): Initialized successfully");
        return true;
    }

    @Override
    public boolean destroy(Map<String, SimpleCustomProperty> configurationAttributes) {
        log.info("Custom Java ScimEventHandler (destroy)");
        scriptLogger.info("Custom Java ScimEventHandler (destroy): Destroying ...");
        scriptLogger.info("Custom Java ScimEventHandler (destroy): Destroyed successfully");
        return true;
    }

    @Override
    public int getApiVersion() {
        return 5;
    }

    @Override
    public boolean createUser(Object user, Map<String, SimpleCustomProperty> configurationAttributes){
        return true;
    }

    @Override
    public boolean postCreateUser(Object user, Map<String, SimpleCustomProperty> configurationAttributes){
        return true;
    }

    @Override
    public boolean updateUser(Object user, Map<String, SimpleCustomProperty> configurationAttributes){
        return true;
    }

    @Override
    public boolean postUpdateUser(Object user, Map<String, SimpleCustomProperty> configurationAttributes){
        return true;
    }

    @Override
    public boolean deleteUser(Object user, Map<String, SimpleCustomProperty> configurationAttributes){
        return true;
    }

    @Override
    public boolean postDeleteUser(Object user, Map<String, SimpleCustomProperty> configurationAttributes){
        return true;
    }

    @Override
    public boolean createGroup(Object group, Map<String, SimpleCustomProperty> configurationAttributes){
        return true;
    }

    @Override
    public boolean postCreateGroup(Object group, Map<String, SimpleCustomProperty> configurationAttributes){
        return true;
    }

    @Override
    public boolean updateGroup(Object group, Map<String, SimpleCustomProperty> configurationAttributes){
        return true;
    }

    @Override
    public boolean postUpdateGroup(Object group, Map<String, SimpleCustomProperty> configurationAttributes){
        return true;
    }

    @Override
    public boolean deleteGroup(Object group, Map<String, SimpleCustomProperty> configurationAttributes){
        return true;
    }

    @Override
    public boolean postDeleteGroup(Object group, Map<String, SimpleCustomProperty> configurationAttributes){
        return true;
    }

    @Override
    public boolean getUser(Object user, Map<String, SimpleCustomProperty> configurationAttributes){
        return true;
    }

    @Override
    public boolean getGroup(Object group, Map<String, SimpleCustomProperty> configurationAttributes){
        return true;
    }

    @Override
    public boolean postSearchUsers(Object results, Map<String, SimpleCustomProperty> configurationAttributes){
        PagedResult res = (PagedResult) results;
        scriptLogger.info("{} entries returned of {}", res.getEntriesCount(), res.getTotalEntriesCount());
        for (Object entry : res.getEntries()) {   
            ScimCustomPerson user = (ScimCustomPerson) entry;     
            scriptLogger.info("Flushing addresses for user {}", user.getUid());
            user.setAttribute("jansAddres", "");
        }
        return true;
    }

    @Override
    public boolean postSearchGroups(Object results, Map<String, SimpleCustomProperty> configurationAttributes){
        return true;
    }

    @Override
    public Response manageResourceOperation(Object context, Object entity, Object payload, Map<String, SimpleCustomProperty> configurationAttributes){
        OperationContext ctx = (OperationContext) context;
        scriptLogger.info("manageResourceOperation. SCIM endpoint invoked is {} (HTTP {})", ctx.getPath(), ctx.getMethod());
        
        String resource_type = ctx.getResourceType();
        if(!resource_type.equals("User")){
            return null;
        }

        String expected_user_type = this.getUserType(ctx.getRequestHeaders());
        ScimCustomPerson ent = (ScimCustomPerson) entity;    
        String jansUsrType = ent.getAttribute("jansUsrTyp");
        
        if(expected_user_type != null && jansUsrType.equals(expected_user_type)){ 
            return null;
        }
        else{
            return new BaseScimWebService().getErrorResponse(403, null, "Attempt to handle a not allowed user type");
        }        
    }

    @Override
    public Response manageSearchOperation(Object context, Object searchRequest, Map<String, SimpleCustomProperty> configurationAttributes){
        OperationContext ctx = (OperationContext) context;
        scriptLogger.info("manageSearchOperation. SCIM endpoint invoked is {} (HTTP {})", ctx.getPath(), ctx.getMethod());

        String resource_type = ctx.getResourceType();
        scriptLogger.info("manageSearchOperation. This is a search over {} resources", resource_type);

        if(!resource_type.equals("User")){
            return null;
        }
            
        String expected_user_type = this.getUserType(ctx.getRequestHeaders());

        if(expected_user_type != null){
            scriptLogger.info("manageSearchOperation. Setting filter to userType eq \"{}\"", expected_user_type);
            ctx.setFilterPrepend("userType eq \"" + expected_user_type + "\"");
            return null;
        }
        else{
            return new BaseScimWebService().getErrorResponse(403, null, "Attempt to handle a not allowed user type");
        }
    }

    // headers params is an instance of javax.ws.rs.core.MultivaluedMap<String, String>
    public String getUserType(MultivaluedMap<String, String> headers){
        String secret = headers.getFirst(custom_header);
        String[] keys = JSONObject.getNames(access_map);

        for(int i = 0; i < keys.length; i++){
            if(keys[i].equals(secret)){
                return access_map.getString(keys[i]);
            }
        }        
        return null;
    }
}
