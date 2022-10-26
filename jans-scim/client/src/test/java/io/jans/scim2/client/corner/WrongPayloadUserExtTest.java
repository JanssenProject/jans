/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.scim2.client.corner;

import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import io.jans.scim2.client.UserBaseTest;

import jakarta.ws.rs.core.Response;

import static jakarta.ws.rs.core.Response.Status.*;

import static org.testng.Assert.*;

/**
 * Created by jgomer on 2017-11-01.
 */
public class WrongPayloadUserExtTest extends UserBaseTest {

    @Parameters({"wrong_user_ext_create_1"})
    @Test
    public void createWrongCustAttrs1(String json){
        Response response=client.createUser(json, null, null);
        assertEquals(response.getStatus(), BAD_REQUEST.getStatusCode());
    }

    @Parameters("wrong_user_ext_create_2")
    @Test
    public void createWrongCustAttrs2(String json){
        Response response=client.createUser(json, null, null);
        assertEquals(response.getStatus(), BAD_REQUEST.getStatusCode());
    }

    @Parameters("wrong_user_ext_create_3")
    @Test
    public void createWrongCustAttrs3(String json){
        Response response=client.createUser(json, null, null);
        assertEquals(response.getStatus(), BAD_REQUEST.getStatusCode());
    }

    @Parameters("wrong_user_ext_create_4")
    @Test
    public void createWrongCustAttrs4(String json){
        Response response=client.createUser(json, null, null);
        assertEquals(response.getStatus(), BAD_REQUEST.getStatusCode());
    }

    @Parameters("wrong_user_ext_create_5")
    @Test
    public void createWrongCustAttrs5(String json){
        Response response=client.createUser(json, null, null);
        assertEquals(response.getStatus(), BAD_REQUEST.getStatusCode());
    }

    @Parameters("wrong_user_ext_create_6")
    @Test
    public void createWrongCustAttrs6(String json){
        Response response=client.createUser(json, null, null);
        assertEquals(response.getStatus(), BAD_REQUEST.getStatusCode());
    }

}
