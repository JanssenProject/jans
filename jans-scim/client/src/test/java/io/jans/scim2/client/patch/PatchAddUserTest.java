/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.scim2.client.patch;

import io.jans.scim.model.scim2.user.UserResource;
import io.jans.scim2.client.UserBaseTest;

import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import jakarta.ws.rs.core.Response;

import static jakarta.ws.rs.core.Response.Status.*;

import static org.testng.Assert.*;

/**
 * Created by jgomer on 2017-11-02.
 */
public class PatchAddUserTest extends UserBaseTest{

    private UserResource user;

    @Parameters({"user_average_create"})
    @Test
    public void createForAdd(String json){
        logger.debug("Creating user from json...");
        user=createUserFromJson(json);
    }

    @Parameters({"user_patchadd"})
    @Test(dependsOnMethods = "createForAdd")
    public void jsonPatch(String patchRequest){
        Response response = client.patchUser(patchRequest, user.getId(), null, null);
        assertEquals(response.getStatus(), OK.getStatusCode());

        UserResource other=response.readEntity(usrClass);

        assertNotNull(other.getNickName());
        assertNotNull(other.getUserType());
        assertTrue(user.getEmails().size() < other.getEmails().size());
        assertTrue(user.getPhoneNumbers().size() < other.getPhoneNumbers().size());

        assertTrue(other.getIms().size()>0);
        assertTrue(other.getRoles().size()>0);
    }

    @Test(dependsOnMethods = "jsonPatch", alwaysRun = true)
    public void delete(){
        deleteUser(user);
    }

}
