/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.scim2.client.search;

import io.jans.scim.model.scim2.ListResponse;
import io.jans.scim.model.scim2.SearchRequest;
import io.jans.scim.model.scim2.user.UserResource;
import io.jans.scim2.client.UserBaseTest;

import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import jakarta.ws.rs.core.Response;

import java.time.Instant;

import static jakarta.ws.rs.core.Response.Status.*;

import static org.testng.Assert.*;

/**
 * Created by jgomer on 2017-10-23.
 */
public class SimpleSearchUserTest extends UserBaseTest {

    private UserResource user;

    @Parameters("user_average_create")
    @Test
    public void create(String json){
        logger.debug("Creating user from json...");
        user=createUserFromJson(json);
    }

    @Test(dependsOnMethods="create", groups = "search")
    public void searchSimpleAttrGet(){

        String isoDateString=user.getMeta().getCreated();
        String locale=user.getLocale();
        logger.debug("Searching user with attribute locale = {} and created date >= {} using GET verb", locale, isoDateString);

        Response response=client.searchUsers(String.format("locale eq \"%s\" and meta.created ge \"%s\"", locale, isoDateString),
                null, null, null, null, null, null);
        assertEquals(response.getStatus(), OK.getStatusCode());

        ListResponse listResponse=response.readEntity(ListResponse.class);
        assertTrue(listResponse.getResources().size()>0);

        //Retrieve first user in results
        UserResource same=listResponse.getResources().stream().map(usrClass::cast).findFirst().get();
        assertEquals(same.getLocale(), locale);

    }

    @Test(dependsOnMethods="create", groups = "search")
    public void searchComplexAttrPost(){

        String givenName=user.getName().getGivenName();
        logger.debug("Searching user with attribute givenName = {} using POST verb", givenName);

        SearchRequest sr=new SearchRequest();
        sr.setFilter("name.givenName eq \""+ givenName + "\"");
        Response response=client.searchUsersPost(sr);
        assertEquals(response.getStatus(), OK.getStatusCode());

        ListResponse listResponse=response.readEntity(ListResponse.class);
        assertNotNull(listResponse);
        assertNotNull(listResponse.getResources());
        assertTrue(listResponse.getResources().size()>0);

        //Retrieve first user in results
        UserResource other =listResponse.getResources().stream().map(usrClass::cast).findFirst().get();
        assertEquals(other.getName().getGivenName(), givenName);

    }

    @Test(dependsOnMethods="create", groups = "search")
    public void searchComplexMultivaluedPost(){

        String ghost = user.getEmails().get(0).getValue();
        final String host = ghost.substring(ghost.indexOf("@")+1);
        logger.debug("Searching user with attribute emails.value like {} or phone numbers with type unassigned or value containing '+' using POST verb", host);

        SearchRequest sr=new SearchRequest();
        sr.setFilter("emails[value ew \"" + host + "\"] or urn:ietf:params:scim:schemas:core:2.0:User:phoneNumbers[value co \"+\" or type eq null]");
        Response response=client.searchUsersPost(sr);
        assertEquals(response.getStatus(), OK.getStatusCode());

        ListResponse listResponse=response.readEntity(ListResponse.class);
        assertTrue(listResponse.getResources().size()>0);

        //Retrieve first user in results
        UserResource other=listResponse.getResources().stream().map(usrClass::cast).findFirst().get();

        boolean cond1 = false, cond2 = false, cond3 = false;
        if (other.getEmails() != null) {
            cond1 = other.getEmails().stream().anyMatch(mail -> mail.getValue().endsWith(host));
        }
        if (other.getPhoneNumbers() != null) {
            cond2 = other.getPhoneNumbers().stream().anyMatch(phone -> phone.getValue().contains("+"));
            cond3 = other.getPhoneNumbers().stream().anyMatch(phone -> phone.getType() == null);
        }
        assertTrue(cond1 || cond2 || cond3);

    }

    @Test(dependsOnMethods="create", groups = "search")
    public void searchNoResults(){

        logger.debug("Calculating the total number of users");
        //Pass count=0 so no results are retrieved (only total)
        Response response=client.searchUsers("userName pr", null, 0, null, null, null, null);
        assertEquals(response.getStatus(), OK.getStatusCode());

        ListResponse listResponse=response.readEntity(ListResponse.class);
        assertNull(listResponse.getResources());

        assertTrue(listResponse.getTotalResults()>0);
        logger.debug("There are {} users!", listResponse.getTotalResults());

    }

    @Test(dependsOnMethods = "create", groups ="search")
    public void searchNoMatches(){

        String nowIsoDateTimeString=Instant.ofEpochMilli(System.currentTimeMillis()).toString();

        SearchRequest sr=new SearchRequest();
        sr.setFilter(String.format("urn:ietf:params:scim:schemas:extension:gluu:2.0:User:scimCustomThird eq 1 and displayName eq \"%s\" " +
                "and addresses[postalCode ne null or type eq null] and meta.lastModified gt \"%s\"", "test", nowIsoDateTimeString));
        Response response=client.searchUsersPost(sr);
        assertEquals(response.getStatus(), OK.getStatusCode());

        ListResponse listResponse=response.readEntity(ListResponse.class);
        assertNull(listResponse.getResources());

    }

    @Test(dependsOnGroups = "search", groups="simple", alwaysRun = true)
    public void delete(){
        deleteUser(user);
    }

}
