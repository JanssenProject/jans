/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.scim2.client;

import io.jans.scim.model.scim2.user.UserResource;

import jakarta.ws.rs.core.Response;

import static jakarta.ws.rs.core.Response.Status.*;

import static org.testng.Assert.*;

/**
 * Created by jgomer on 2017-10-23.
 */
public class UserBaseTest extends BaseTest {

    protected static final Class<UserResource> usrClass=UserResource.class;

    public UserResource createUserFromJson(String json){

        Response response=client.createUser(json, null, null);
        assertEquals(response.getStatus(), CREATED.getStatusCode());

        UserResource user=response.readEntity(usrClass);
        assertNotNull(user.getMeta());
        logger.debug("User created with id {}", user.getId());

        return user;
    }

    public void deleteUser(UserResource user){

        logger.debug("Deleting user {}", user.getUserName());
        Response response=client.deleteUser(user.getId());
        assertEquals(response.getStatus(), NO_CONTENT.getStatusCode());
        logger.debug("deleted");

    }

}
