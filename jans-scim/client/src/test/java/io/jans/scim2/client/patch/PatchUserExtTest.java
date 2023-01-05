/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.scim2.client.patch;

import io.jans.scim.model.scim2.CustomAttributes;
import io.jans.scim.model.scim2.patch.PatchOperation;
import io.jans.scim.model.scim2.patch.PatchRequest;
import io.jans.scim.model.scim2.user.PhoneNumber;
import io.jans.scim.model.scim2.user.UserResource;
import io.jans.scim.model.scim2.util.DateUtil;
import io.jans.scim2.client.UserBaseTest;

import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import jakarta.ws.rs.core.Response;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static io.jans.scim.model.scim2.Constants.USER_EXT_SCHEMA_ID;
import static jakarta.ws.rs.core.Response.Status.*;
import static org.testng.Assert.*;

/**
 * Created by jgomer on 2017-11-02.
 */
public class PatchUserExtTest extends UserBaseTest {

    private UserResource user;

    @Parameters({"user_full_create"})
    @Test
    public void create(String json) {
        logger.debug("Creating user from json...");
        user=createUserFromJson(json);
    }

    @Parameters({"user_patch_ext"})
    @Test(dependsOnMethods = "create")
    public void patchJson(String patchRequest){

        Response response = client.patchUser(patchRequest, user.getId(), null, null);
        assertEquals(response.getStatus(), OK.getStatusCode());

        UserResource other=response.readEntity(usrClass);
        //For help on usage of io.jans.scim.model.scim2.CustomAttributes class, read its api docs (oxtrust-scim maven project)
        CustomAttributes custAttrs=other.getCustomAttributes(USER_EXT_SCHEMA_ID);

        //Verify new items appeared in scimCustomSecond
        List<Date> scimCustomSecond=custAttrs.getValues("scimCustomSecond", Date.class);
        assertEquals(scimCustomSecond.size(), 6);

        //Verify change in value of scimCustomThird
        int scimCustomThird=custAttrs.getValue("scimCustomThird", Integer.class);
        assertEquals(1, scimCustomThird);

        //Verify scimCustomFirst disappeared
        assertNull(custAttrs.getValue("scimCustomFirst", String.class));

        //Verify some others disappeared too
        assertNull(other.getAddresses().get(0).getType());
        assertNull(other.getName().getGivenName());

        Stream<String> types=other.getPhoneNumbers().stream().map(PhoneNumber::getType);
        assertTrue(types.map(Optional::ofNullable).noneMatch(Optional::isPresent));

    }

    @Test(dependsOnMethods = "patchJson")
    public void patchObject(){

        PatchOperation operation=new PatchOperation();
        operation.setOperation("replace");
        operation.setPath("urn:ietf:params:scim:schemas:extension:gluu:2.0:User:scimCustomSecond");

        long now=System.currentTimeMillis();
        List<String> someDates= Arrays.asList(now, now+1000, now+2000, now+3000).stream()
                .map(DateUtil::millisToISOString).collect(Collectors.toList());
        operation.setValue(someDates);

        PatchRequest pr=new PatchRequest();
        pr.setOperations(Collections.singletonList(operation));

        Response response=client.patchUser(pr, user.getId(), null, null);
        assertEquals(response.getStatus(), OK.getStatusCode());

        UserResource other=response.readEntity(usrClass);
        CustomAttributes custAttrs=other.getCustomAttributes(USER_EXT_SCHEMA_ID);

        //Verify different dates appeared in scimCustomSecond
        List<Date> scimCustomSecond=custAttrs.getValues("scimCustomSecond", Date.class);
        assertEquals(scimCustomSecond.size(), someDates.size());
        assertEquals(now, scimCustomSecond.get(0).getTime());

    }

    @Test(dependsOnMethods = "patchObject", alwaysRun = true)
    public void delete(){
        deleteUser(user);
    }

}
