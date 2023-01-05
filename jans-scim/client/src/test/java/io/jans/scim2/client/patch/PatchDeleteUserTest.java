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
public class PatchDeleteUserTest extends UserBaseTest{

    private UserResource user;

    @Parameters({"user_full_create"})
    @Test
    public void createForDel(String json){
        logger.debug("Creating user from json...");
        user=createUserFromJson(json);
    }

    @Parameters({"user_patchdelete"})
    @Test(dependsOnMethods = "createForDel")
    public void delete1(String patchRequest){

        Response response = client.patchUser(patchRequest, user.getId(), null, null);
        assertEquals(response.getStatus(), OK.getStatusCode());

        UserResource other=response.readEntity(usrClass);
        for (int i = 0 ; i < 2; i++) {
            assertNull(other.getName().getMiddleName());
            assertNull(other.getNickName());
            assertNull(other.getEntitlements());

            assertNull(other.getAddresses().get(0).getPostalCode());
            assertNull(other.getAddresses().get(0).getLocality());
            assertNotNull(other.getAddresses().get(0).getStreetAddress());

            //Double check
            response = client.getUserById(user.getId(), null, null);
            other = response.readEntity(usrClass);
        }

    }

    @Test(dependsOnMethods = "delete1", alwaysRun = true)
    public void delete(){
        deleteUser(user);
    }

}
