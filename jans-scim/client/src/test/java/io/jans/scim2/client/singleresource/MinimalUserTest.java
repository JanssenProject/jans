/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.scim2.client.singleresource;

import io.jans.scim.model.scim2.user.UserResource;
import io.jans.scim2.client.UserBaseTest;

import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import jakarta.ws.rs.core.Response;

import static jakarta.ws.rs.core.Response.Status.*;

import static org.testng.Assert.*;

/**
 * Created by jgomer on 2017-10-21.
 */
public class MinimalUserTest extends UserBaseTest {

    private UserResource user;

    @Parameters("user_minimal_create")
    @Test
    public void createUser(String json){
        logger.debug("Creating mimimal user from json...");
        user = createUserFromJson(json);
    }

    @Parameters("user_minimal_update")
    @Test(dependsOnMethods="createUser")
    public void updateUser(String json){

        logger.debug("Updating user {} with json", user.getUserName());
        Response response=client.updateUser(json, user.getId(), null, null);
        assertEquals(response.getStatus(), OK.getStatusCode());

        user=response.readEntity(usrClass);
        assertNotNull(user.getName());
        assertTrue(user.getActive());
        logger.debug("Updated user {}", user.getName().getGivenName());

    }

    @Test(dependsOnMethods="updateUser", alwaysRun = true)
    public void delete(){
        deleteUser(user);
    }

}
