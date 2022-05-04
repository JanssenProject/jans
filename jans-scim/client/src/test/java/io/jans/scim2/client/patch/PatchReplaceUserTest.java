/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.scim2.client.patch;

import io.jans.scim.model.scim2.patch.PatchOperation;
import io.jans.scim.model.scim2.patch.PatchRequest;
import io.jans.scim.model.scim2.user.Address;
import io.jans.scim.model.scim2.user.InstantMessagingAddress;
import io.jans.scim.model.scim2.user.PhoneNumber;
import io.jans.scim.model.scim2.user.UserResource;
import io.jans.scim2.client.UserBaseTest;

import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import jakarta.ws.rs.core.Response;

import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

import static jakarta.ws.rs.core.Response.Status.*;

import static org.testng.Assert.*;

/**
 * Note: these cases depend heavily on the contents and structure of files under /single/patch/
 * Created by jgomer on 2017-11-02.
 */
public class PatchReplaceUserTest extends UserBaseTest {

    private UserResource user;

    @Parameters({"user_full_create"})
    @Test
    public void createForReplace(String json){
        logger.debug("Creating user from json...");
        user=createUserFromJson(json);
    }

    @Parameters({"user_patchreplace_1"})
    @Test(dependsOnMethods = "createForReplace")
    public void jsonNoPathPatch1(String patchRequest){

        Response response = client.patchUser(patchRequest, user.getId(), null, null);
        assertEquals(response.getStatus(), OK.getStatusCode());

        UserResource other=response.readEntity(usrClass);
        //Verify display name changed
        assertNotEquals(user.getDisplayName(), other.getDisplayName());
        //Verify some name components changed
        assertNotEquals(user.getName().getFamilyName(), other.getName().getFamilyName());
        assertNotEquals(user.getName().getMiddleName(), other.getName().getMiddleName());
        assertNotEquals(user.getName().getGivenName(), other.getName().getGivenName());

        assertEquals(user.getName().getHonorificPrefix(), other.getName().getHonorificPrefix());
        assertEquals(user.getName().getHonorificSuffix(), other.getName().getHonorificSuffix());

        //Verify is not active
        assertFalse(other.getActive());

        //Verify title changed
        assertNotEquals(user.getTitle(), other.getTitle());

        user=other;

    }

    @Parameters({"user_patchreplace_2"})
    @Test(dependsOnMethods = "jsonNoPathPatch1")
    public void jsonNoPathPatch2(String patchRequest){

        Response response = client.patchUser(patchRequest, user.getId(), null, null);
        assertEquals(response.getStatus(), OK.getStatusCode());

        UserResource other=response.readEntity(usrClass);
        assertNotEquals(user.getName().getHonorificPrefix(), other.getName().getHonorificPrefix());
        assertNotEquals(user.getName().getHonorificSuffix(), other.getName().getHonorificSuffix());
        assertNotEquals(user.getName().getFormatted(), other.getName().getFormatted());

        //Verify change in the streetAddress
        Address adr=other.getAddresses().get(0);
        assertNotEquals(user.getAddresses().get(0).getStreetAddress(), adr.getStreetAddress());

        //Verify other attributes were nulled
        assertNull(adr.getPostalCode());
        assertNull(adr.getRegion());

        //Verify change in number of phone numbers
        assertNotEquals(user.getPhoneNumbers().size(), other.getPhoneNumbers().size());

        //Verify new user has different phone numbers
        String phone=user.getPhoneNumbers().get(0).getValue();
        assertTrue(other.getPhoneNumbers().stream().map(PhoneNumber::getValue).noneMatch(phone::equals));

        //Verify x509Certs disappeared
        assertNull(other.getX509Certificates());

        //Verify "roles" are still there intact
        assertEquals(user.getRoles().size(), other.getRoles().size());
        assertEquals(user.getRoles().get(0).getValue(), other.getRoles().get(0).getValue());

        user=other;
    }

    @Parameters({"user_patchreplace_3"})
    @Test(dependsOnMethods = "jsonNoPathPatch2")
    public void jsonPathPatch1(String patchRequest){

        Response response = client.patchUser(patchRequest, user.getId(), null, null);
        assertEquals(response.getStatus(), OK.getStatusCode());

        UserResource other=response.readEntity(usrClass);
        //Verify display name changed
        assertNotEquals(user.getDisplayName(), other.getDisplayName());
        //Verify some name components changed
        assertNotEquals(user.getName().getFamilyName(), other.getName().getFamilyName());
        assertNotEquals(user.getName().getMiddleName(), other.getName().getMiddleName());
        assertNotEquals(user.getName().getGivenName(), other.getName().getGivenName());

        assertEquals(user.getName().getHonorificPrefix(), other.getName().getHonorificPrefix());
        assertEquals(user.getName().getHonorificSuffix(), other.getName().getHonorificSuffix());

        //Verify is now active
        assertTrue(other.getActive());

        user=other;

    }

    @Parameters({"user_patchreplace_4"})
    @Test(dependsOnMethods = "jsonPathPatch1")
    public void jsonPathPatch2(String patchRequest){

        Response response = client.patchUser(patchRequest, user.getId(), null, null);
        assertEquals(response.getStatus(), OK.getStatusCode());

        UserResource other=response.readEntity(usrClass);
        assertNotEquals(user.getName().getHonorificPrefix(), other.getName().getHonorificPrefix());
        assertNotEquals(user.getName().getHonorificSuffix(), other.getName().getHonorificSuffix());
        assertNotEquals(user.getName().getFormatted(), other.getName().getFormatted());

        //Verify change in the streetAddress
        Address adr=other.getAddresses().get(0);
        assertNotEquals(user.getAddresses().get(0).getStreetAddress(), adr.getStreetAddress());
        //Verify postal code is there
        assertNotNull(adr.getPostalCode());

        //Verify change in phone number values
        int num=user.getPhoneNumbers().size();
        assertEquals(num, other.getPhoneNumbers().size());
        Set<String> set1=user.getPhoneNumbers().stream().map(PhoneNumber::getValue).collect(Collectors.toSet());
        Set<String> set2=other.getPhoneNumbers().stream().map(PhoneNumber::getValue).collect(Collectors.toSet());
        assertFalse(set2.removeAll(set1));

        //Verify x509Certs disappeared
        assertNotNull(other.getX509Certificates());

        user=other;

    }

    @Test(dependsOnMethods = "jsonPathPatch2")
    public void objectPatch(){

        //Create a patch request by supplying a singleton list with one IMS object
        InstantMessagingAddress ims=new InstantMessagingAddress();
        ims.setDisplay("barbas");
        ims.setPrimary(true);
        ims.setType("escape");
        ims.setValue("bjensen");

        PatchOperation op=new PatchOperation();
        op.setOperation("replace");
        op.setPath("ims");
        op.setValue(Collections.singleton(ims));

        PatchRequest pr=new PatchRequest();
        pr.setOperations(Collections.singletonList(op));

        Response response = client.patchUser(pr, user.getId(), null, null);
        assertEquals(response.getStatus(), OK.getStatusCode());

        UserResource other=response.readEntity(usrClass);
        for (int i = 0; i < 2; i++) {
            //Verify different info appeared
            InstantMessagingAddress newIms = other.getIms().get(0);
            assertEquals(newIms.getDisplay(), ims.getDisplay());
            assertEquals(newIms.getValue(), ims.getValue());
            assertEquals(newIms.getType(), ims.getType());
            assertEquals(newIms.getPrimary(), ims.getPrimary());

            //Double check
            response = client.getUserById(user.getId(), "ims", null);
            other=response.readEntity(usrClass);
        }

    }

    @Test(dependsOnMethods = "objectPatch", alwaysRun = true)
    public void delete(){
        deleteUser(user);
    }

}
