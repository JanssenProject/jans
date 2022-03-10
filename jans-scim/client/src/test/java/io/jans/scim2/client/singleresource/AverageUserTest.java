/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.scim2.client.singleresource;

import io.jans.scim.model.scim2.user.Email;
import io.jans.scim.model.scim2.user.Group;
import io.jans.scim.model.scim2.user.PhoneNumber;
import io.jans.scim.model.scim2.user.UserResource;
import io.jans.scim2.client.UserBaseTest;

import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import jakarta.ws.rs.core.Response;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Optional;

import static jakarta.ws.rs.core.Response.Status.*;

import static org.testng.Assert.*;

/**
 * Created by jgomer on 2017-10-22.
 */
public class AverageUserTest extends UserBaseTest {

    private UserResource user;

    @Parameters("user_average_create")
    @Test
    public void create(String json) {
        logger.debug("Creating user from json...");
        user = createUserFromJson(json);
    }

    @Parameters("user_average_update")
    @Test(dependsOnMethods = "create")
    public void updateWithJson(String json) {

        logger.debug("Updating user {} with json", user.getUserName());
        Response response = client.updateUser(json, user.getId(), null, null);
        assertEquals(response.getStatus(), OK.getStatusCode());

        user = response.readEntity(usrClass);
        assertFalse(user.getRoles().isEmpty());
        assertTrue(user.getPhoneNumbers().size() > 1);

        //Attest there is at most ONE primary phone number despite any data passed
        long primaryTrueCount = user.getPhoneNumbers().stream().map(pn -> Optional.ofNullable(pn.getPrimary()).orElse(false))
                .filter(Boolean::booleanValue).count();
        assertTrue(primaryTrueCount < 2);

        logger.debug("Updated user {}", user.getName().getGivenName());

    }

    @Test(dependsOnMethods = "updateWithJson")
    public void updateWithObject1() throws Exception {

        UserResource clone = getDeepCloneUsr(user);
        clone.setPreferredLanguage("en_US");
        clone.getPhoneNumbers().remove(0);
        clone.setAddresses(null);   //Means no change
        clone.setRoles(new ArrayList<>());  //Means role will have to disappear

        Group group = new Group();
        group.setValue("Dummy ID");
        clone.setGroups(Collections.singletonList(group));  //will be ignored: group membership changes MUST be applied via /Groups endpoint

        logger.debug("Updating user {}", clone.getUserName());
        Response response = client.updateUser(clone, clone.getId(), null, "meta");
        assertEquals(response.getStatus(), OK.getStatusCode());

        user = response.readEntity(usrClass);
        assertNotNull(user.getPreferredLanguage());
        assertEquals(user.getPreferredLanguage(), clone.getPreferredLanguage());
        assertEquals(user.getPhoneNumbers().size(), clone.getPhoneNumbers().size());
        assertFalse(user.getAddresses().isEmpty());
        assertNull(user.getRoles());
        assertNull(user.getGroups());

        logger.debug("Updated user {}", user.getName().getGivenName());

        //Double check update did take update in the original source (eg. LDAP):
        String json = response.readEntity(String.class);
        response = client.getUserById(clone.getId(), null, "meta");
        //both json contents should be the same since meta attribute was removed and serialization involves UserResource class
        assertEquals(json, response.readEntity(String.class));

    }

    @Test(dependsOnMethods = "updateWithObject1")
    public void updateWithObject2() {

        UserResource aUser = new UserResource();
        aUser.setEmails(user.getEmails());
        aUser.setPhoneNumbers(user.getPhoneNumbers());

        //Change some canonical values
        aUser.getEmails().get(0).setType(Email.Type.HOME);
        aUser.getPhoneNumbers().get(0).setType("fax");

        PhoneNumber pager = new PhoneNumber();
        pager.setValue("+1 234 566 9999");
        pager.setType(PhoneNumber.Type.PAGER);
        aUser.getPhoneNumbers().add(pager);

        Response response = client.updateUser(aUser, user.getId(), "emails, addresses, phoneNumbers, userName", null);
        user = response.readEntity(UserResource.class);

        assertEquals(user.getEmails().get(0).getType(), Email.Type.HOME.name().toLowerCase());
        assertEquals(user.getPhoneNumbers().get(0).getType(), PhoneNumber.Type.FAX.name().toLowerCase());
        assertEquals(user.getPhoneNumbers().get(1).getType(), PhoneNumber.Type.PAGER.name().toLowerCase());
        assertNull(user.getPreferredLanguage());

        logger.debug("Updated user {}", user.getUserName());

    }

    @Test(dependsOnMethods = "updateWithObject2", alwaysRun = true, groups="avgTestFinished")
    public void delete() {
        deleteUser(user);
    }

}
