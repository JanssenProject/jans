/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.scim2.client.patch;

import io.jans.scim.model.scim2.patch.PatchOperation;
import io.jans.scim.model.scim2.patch.PatchRequest;
import io.jans.scim.model.scim2.user.Address;
import io.jans.scim.model.scim2.user.Email;
import io.jans.scim.model.scim2.user.PhoneNumber;
import io.jans.scim.model.scim2.user.UserResource;
import io.jans.scim2.client.UserBaseTest;

import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import jakarta.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.Collections;

import static jakarta.ws.rs.core.Response.Status.*;

import static org.testng.Assert.*;

/**
 * Created by jgomer on 2017-12-16.
 */
public class PatchValueFilterUserTest extends UserBaseTest {

    private UserResource user;

    @Test
    public void create(){
        user=getDummyUser();
        Response response=client.createUser(user, "id", null);
        assertEquals(response.getStatus(), CREATED.getStatusCode());

        user=response.readEntity(usrClass);
        logger.debug("User created with id {}", user.getId());
    }

    @Parameters("user_patch_valuefilter")
    @Test(dependsOnMethods = "create")
    public void patch(String json){

        Response response=client.patchUser(json, user.getId(), null, null);
        assertEquals(response.getStatus(), OK.getStatusCode());
        user=response.readEntity(usrClass);

        //Verify changes: emails
        assertTrue(user.getEmails().stream().allMatch(email -> email.getDisplay().equals("bjane")));
        assertTrue(user.getEmails().stream().allMatch(email -> email.getType().equals("hobby")));
        //phone numbers
        assertTrue(user.getPhoneNumbers().stream().allMatch(pn -> pn.getValue().equals("+1 1234 56789")));

    }

    @Test(dependsOnMethods = "patch")
    public void objectPatch(){

        PatchRequest request=new PatchRequest();
        request.setOperations(new ArrayList<>());

        PatchOperation del=new PatchOperation();
        del.setOperation("remove");
        del.setPath("emails[type sw \"hobby\"]");
        request.getOperations().add(del);

        del=new PatchOperation();
        del.setOperation("remove");
        del.setPath("phoneNumbers[primary pr or value co \" \"].type");
        request.getOperations().add(del);

        del=new PatchOperation();
        del.setOperation("remove");
        del.setPath("addresses[region eq \"somewhere\" and primary ne true].locality");
        request.getOperations().add(del);

        Response response = client.patchUser(request, user.getId(), null, null);
        assertEquals(response.getStatus(), OK.getStatusCode());

        user=response.readEntity(usrClass);
        assertNull(user.getEmails());
        assertTrue(user.getPhoneNumbers().stream().allMatch(ph -> ph.getType()==null));
        assertEquals(user.getAddresses().size(), 1);    //No change in addresses
        assertNull(user.getAddresses().get(0).getLocality());

    }

    @Test(dependsOnMethods = "objectPatch", alwaysRun = true)
    public void delete(){
        deleteUser(user);
    }

    private UserResource getDummyUser() {

        user = new UserResource();
        user.setUserName("" + System.currentTimeMillis());
        user.setEmails(new ArrayList<>());
        user.setPhoneNumbers(new ArrayList<>());

        Email email = new Email();
        email.setValue("bjane@gluu.org");
        email.setDisplay("bjane");
        email.setPrimary(true);
        email.setType("work");
        user.getEmails().add(email);

        email = new Email();
        email.setValue("bjane@acme.org");
        email.setDisplay("bjane acme");
        email.setType("hobby");
        user.getEmails().add(email);

        PhoneNumber pn = new PhoneNumber();
        pn.setValue("+57 1234 56789");
        pn.setType("home");
        user.getPhoneNumbers().add(pn);

        pn = new PhoneNumber();
        pn.setValue("+1 1234 56789");
        pn.setType("work");
        pn.setPrimary(true);
        user.getPhoneNumbers().add(pn);

        Address address = new Address();
        address.setCountry("EG");
        address.setPrimary(false);
        address.setRegion("Somewhere");
        address.setStreetAddress("59 Acacia avenue");
        address.setLocality("Donington");
        user.setAddresses(Collections.singletonList(address));

        return user;

    }

}
