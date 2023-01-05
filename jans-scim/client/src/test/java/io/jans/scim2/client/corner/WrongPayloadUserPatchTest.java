/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.scim2.client.corner;

import io.jans.scim.model.scim2.user.UserResource;
import io.jans.scim2.client.UserBaseTest;

import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import jakarta.ws.rs.core.Response;

import static jakarta.ws.rs.core.Response.Status.*;

import static org.testng.Assert.*;

/**
 * Created by jgomer on 2017-12-28.
 */
public class WrongPayloadUserPatchTest extends UserBaseTest {

    private UserResource user;

    @Parameters("user_average_create")
    @Test
    public void create(String json) {
        logger.debug("Creating user from json...");
        user = createUserFromJson(json);
    }

    @Parameters("wrong_user_patch")
    @Test
    public void patch(String patchRequest) {
        Response response = client.patchUser(patchRequest, user.getId(), null, null);
        assertNotEquals(response.getStatus(), OK.getStatusCode());
    }

    @Test(dependsOnMethods = "patch", alwaysRun = true)
    public void delete() {
        deleteUser(user);
    }

}
