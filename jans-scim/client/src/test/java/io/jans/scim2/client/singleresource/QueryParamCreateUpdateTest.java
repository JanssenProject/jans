/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.scim2.client.singleresource;

import io.jans.scim.model.scim2.CustomAttributes;
import io.jans.scim.model.scim2.user.Email;
import io.jans.scim.model.scim2.user.UserResource;
import io.jans.scim2.client.UserBaseTest;

import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import jakarta.ws.rs.core.Response;

import java.util.Collections;
import java.util.Date;
import java.util.Map;
import java.util.Optional;

import static io.jans.scim.model.scim2.Constants.*;
import static jakarta.ws.rs.core.Response.Status.*;

import static org.testng.Assert.*;

/**
 * Created by jgomer on 2017-11-12.
 */
public class QueryParamCreateUpdateTest extends UserBaseTest {

    private UserResource user;

    @Parameters("user_full_create")
    @Test
    public void create1(String json){
        logger.debug("Creating user from json...");

        String include="displayName, emails.value, password, nickName, urn:ietf:params:scim:schemas:core:2.0:User:name.givenName, " +
                "preferredLanguage, userName, urn:ietf:params:scim:schemas:extension:gluu:2.0:User:scimCustomSecond, " +
                "urn:ietf:params:scim:schemas:extension:gluu:2.0:User:scimCustomThird";
        Response response=client.createUser(json, include, null);
        assertEquals(response.getStatus(), CREATED.getStatusCode());

        user=response.readEntity(usrClass);
        execAssertions(user);

    }

    @Test(dependsOnMethods = "create1", alwaysRun = true)
    public void delete1(){
        deleteUser(user);
    }

    @Parameters("user_full_create")
    @Test(dependsOnMethods = "delete1")
    public void create2(String json){
        logger.debug("Creating user from json...");

        String exclude="schemas, id, externalId, name.honorificPrefix, name.honorificSuffix, name.formatted, name.familyName, name.middleName, " +
                "profileUrl, emails.display, emails.primary, emails.type, preferredLanguage, addresses, phoneNumbers, ims, userType, title, active, " +
                "roles, entitlements, x509Certificates, urn:ietf:params:scim:schemas:extension:gluu:2.0:User:scimCustomFirst";
        Response response=client.createUser(json, null, exclude);
        assertEquals(response.getStatus(), CREATED.getStatusCode());

        user=response.readEntity(usrClass);
        execAssertions(user);

    }

    @Test(dependsOnMethods = "create2")
    public void update1() throws Exception{

        //Change some attributes existing in user object
        UserResource cheapClone=getDeepCloneUsr(user);
        cheapClone.getName().setGivenName("Bavara");
        cheapClone.setNickName("Cloned");

        String rndString=Double.toString(Math.random());
        //For help on usage of io.jans.scim.model.scim2.CustomAttributes class, read its api docs (oxtrust-scim maven project)
        CustomAttributes custAttrs=cheapClone.getCustomAttributes(USER_EXT_SCHEMA_ID);
        custAttrs.setAttribute("scimCustomFirst", rndString);

        String include="userName, name.givenName, nickName, urn:ietf:params:scim:schemas:extension:gluu:2.0:User:scimCustomFirst";
        Response response=client.updateUser(cheapClone, cheapClone.getId(), include, null);
        assertEquals(response.getStatus(), OK.getStatusCode());

        user=response.readEntity(usrClass);

        assertNull(user.getDisplayName());
        assertEquals(user.getName().getGivenName(), cheapClone.getName().getGivenName());
        assertEquals(user.getNickName(), cheapClone.getNickName());

        custAttrs=user.getCustomAttributes(USER_EXT_SCHEMA_ID);
        assertNull(custAttrs.getValues("scimCustomSecond", Date.class));
        assertNull(custAttrs.getValue("scimCustomThird", Integer.class));
        assertEquals(custAttrs.getValue("scimCustomFirst", String.class), rndString);

    }

    @Test(dependsOnMethods = "update1")
    public void update2() throws Exception{

        UserResource cheapClone=getDeepCloneUsr(user);
        cheapClone.setEmails(Collections.emptyList());
        cheapClone.setAddresses(Collections.emptyList());
        cheapClone.setPhoneNumbers(Collections.emptyList());
        cheapClone.setIms(Collections.emptyList());
        cheapClone.setRoles(Collections.emptyList());
        cheapClone.setEntitlements(Collections.emptyList());
        cheapClone.setX509Certificates(Collections.emptyList());

        String exclude="urn:ietf:params:scim:schemas:extension:gluu:2.0:User:scimCustomFirst, urn:ietf:params:scim:schemas:core:2.0:User:active, " +
                "urn:ietf:params:scim:schemas:extension:gluu:2.0:User:scimCustomSecond, externalId, userName, name, " +
                "urn:ietf:params:scim:schemas:extension:gluu:2.0:User:scimCustomThird, userType, title, profileUrl";

        Response response=client.updateUser(cheapClone, cheapClone.getId(), null, exclude);
        assertEquals(response.getStatus(), OK.getStatusCode());

        user=response.readEntity(usrClass);

        assertNotNull(user.getDisplayName());
        assertNotNull(user.getNickName());

        //Verify excluded are not present
        assertNull(user.getCustomAttributes(USER_EXT_SCHEMA_ID));
        assertNull(user.getExternalId());
        assertNull(user.getUserName());
        assertNull(user.getName());
        assertNull(user.getProfileUrl());
        assertNull(user.getUserType());
        assertNull(user.getTitle());
        assertNull(user.getActive());

        //Verify update took place really
        assertNull(user.getEmails());
        assertNull(user.getAddresses());
        assertNull(user.getPhoneNumbers());
        assertNull(user.getIms());
        assertNull(user.getRoles());
        assertNull(user.getEntitlements());
        assertNull(user.getX509Certificates());

    }

    @Test(dependsOnMethods = "update2", alwaysRun = true)
    public void delete(){
        deleteUser(user);
    }

    private void execAssertions(UserResource user){
        //Verify "ALWAYS" attribs were retrieved
        assertNotNull(user.getSchemas());
        assertNotNull(user.getId());

        //Verify no password was retrieved
        assertNull(user.getPassword());

        //Verify preferredLanguage is null (not provided in Json originally)
        assertNull(user.getPreferredLanguage());

        //Verify all others are present
        assertNotNull(user.getUserName());
        assertNotNull(user.getDisplayName());
        assertNotNull(user.getNickName());
        assertNotNull(user.getName().getGivenName());

        //Verify cust attrs are there
        CustomAttributes custAttrs=user.getCustomAttributes(USER_EXT_SCHEMA_ID);
        assertNotNull(custAttrs.getValues("scimCustomSecond", Date.class));
        assertNotNull(custAttrs.getValue("scimCustomThird", Integer.class));

        //Verify e-mails were retrieved
        assertNotNull(user.getEmails());
        assertTrue(user.getEmails().stream().map(Email::getValue).map(Optional::ofNullable).allMatch(Optional::isPresent));

    }

}
