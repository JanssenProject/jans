/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.scim2.client.singleresource;

import io.jans.scim.model.scim2.CustomAttributes;
import io.jans.scim.model.scim2.ListResponse;
import io.jans.scim.model.scim2.SearchRequest;
import io.jans.scim.model.scim2.user.Name;
import io.jans.scim.model.scim2.user.UserResource;
import io.jans.scim2.client.UserBaseTest;
import io.jans.scim2.listener.SkipTest;

import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import jakarta.ws.rs.core.Response;
import java.util.*;

import static io.jans.scim.model.scim2.Constants.USER_EXT_SCHEMA_ID;
import static jakarta.ws.rs.core.Response.Status.*;

import static org.testng.Assert.*;

/**
 * Created by jgomer on 2017-11-01.
 */
public class FullUserTest extends UserBaseTest {

    private UserResource user;

    @Parameters("user_full_create")
    @Test(dependsOnGroups="avgTestFinished")
    public void createFull(String json) {
        logger.debug("Creating user from json...");
        user=createUserFromJson(json);

        //Confirm extended attrs info is there
        //For help on usage of io.jans.scim.model.scim2.CustomAttributes class, read its api docs (oxtrust-scim maven project)
        CustomAttributes custAttrs=user.getCustomAttributes(USER_EXT_SCHEMA_ID);

        assertNotNull(custAttrs.getValue("scimCustomFirst", String.class));
        assertNotNull(custAttrs.getValues("scimCustomSecond", Date.class));
        assertNotNull(custAttrs.getValue("scimCustomThird", Integer.class));
        assertEquals(custAttrs.getValues("scimCustomSecond", Date.class).size(), 1);

    }

    @Parameters("user_full_update")
    @Test(dependsOnMethods = "createFull")
    public void update(String json) {

        logger.debug("Updating user {} with json", user.getUserName());
        Response response=client.updateUser(json, user.getId(), null, null);
        assertEquals(response.getStatus(), OK.getStatusCode());

        UserResource other=response.readEntity(usrClass);
        CustomAttributes custAttrs1=user.getCustomAttributes(USER_EXT_SCHEMA_ID);
        CustomAttributes custAttrs2=other.getCustomAttributes(USER_EXT_SCHEMA_ID);

        //Verify scimCustomFirst changed
        assertNotEquals(custAttrs1.getValue("scimCustomFirst", String.class), custAttrs2.getValue("scimCustomFirst", String.class));

        //Verify a new scimCustomSecond value
        List<Date> col1=custAttrs1.getValues("scimCustomSecond", Date.class);
        List<Date> col2=custAttrs2.getValues("scimCustomSecond", Date.class);
        assertNotEquals(col1.size(), col2.size());

        //Verify scimCustomThird is the same
        assertEquals(custAttrs1.getValue("scimCustomThird", Integer.class), custAttrs2.getValue("scimCustomThird", Integer.class));

        //Verify change in emails, addresses and phoneNumbers
        assertNotEquals(user.getEmails().size(), other.getEmails().size());
        assertNotEquals(user.getAddresses().size(), other.getAddresses().size());
        assertNotEquals(user.getPhoneNumbers().size(), other.getPhoneNumbers().size());

        //Verify x509Certificates disappeared
        assertNull(other.getX509Certificates());

        //Verify no change in user type
        assertEquals(user.getUserType(), other.getUserType());

        user=other;

    }

    @Test(dependsOnMethods="update", groups = "lastTests")
    public void updateNonExisting(){

        //Set values missing in the user so far
        user.setPreferredLanguage("en-us");
        user.setLocale("en_US");

        //Don't remove name from the list (needed in the next case :P)
        Response response=client.updateUser(user, user.getId(), "preferredLanguage, locale, name", null);
        assertEquals(response.getStatus(), OK.getStatusCode());

        user=response.readEntity(usrClass);
        assertNotNull(user.getPreferredLanguage());
        assertNotNull(user.getLocale());

    }

    @SkipTest(databases = { "couchbase", "spanner" })
    @Test(dependsOnMethods="updateNonExisting", groups = "lastTests")
    public void searchEscapingChars() {

        char quote = '"', backslash = '\\';
        String scapedQuote = String.valueOf(new char[]{backslash, quote});
        String scapedBkSlash = String.valueOf(new char[]{backslash, backslash});
        //Used to generate a random Unicode char
        String rnd = UUID.randomUUID().toString().substring(0, 4);
        logger.debug("Using random unicode character (HEX): {}", rnd);
        String unicodeStr = String.valueOf(Character.toChars(Integer.parseInt(rnd, 16)));

        Name name = user.getName();
        name.setGivenName(String.format("with %cquotes%c", quote, quote));
        name.setMiddleName(String.format("with backslash %c", backslash));
        name.setFamilyName(String.format("%c %c %s", quote, backslash, unicodeStr));

        CustomAttributes attrs = new CustomAttributes(USER_EXT_SCHEMA_ID);
        attrs.setAttribute("scimCustomFirst", String.valueOf(quote));
        user.addCustomAttributes(attrs);

        Response response = client.updateUser(user, user.getId(), null, null);
        assertEquals(response.getStatus(), OK.getStatusCode());

        String filter = String.format("name.givenName co %c%s%c", quote, scapedQuote, quote);   // => name.givenName co "\""
        filter += String.format(" and name.middleName ew %c%s%c", quote, scapedBkSlash, quote);  // => and name.middleName ew "\\"

        String compValue = String.format("%s %s %cu%s", scapedQuote, scapedBkSlash, backslash, rnd);
        filter += String.format(" and name.familyName eq %c%s%c", quote, compValue, quote);  // => and name.familyName eq ""\ \\ \\uWXYZ"

        String customFirst = String.format("%s:%s", USER_EXT_SCHEMA_ID, "scimCustomFirst");
        filter += String.format(" and %s eq %c%s%c", customFirst, quote, scapedQuote, quote);

        SearchRequest sr = new SearchRequest();
        sr.setFilter(filter);
        sr.setCount(1);
        sr.setAttributes("name, " + customFirst);

        response = client.searchUsersPost(sr);
        user = (UserResource) response.readEntity(ListResponse.class).getResources().get(0);

        assertEquals(name.getGivenName(), user.getName().getGivenName());
        assertEquals(name.getMiddleName(), user.getName().getMiddleName());
        assertEquals(name.getFamilyName(), user.getName().getFamilyName());

        //Verify the unicode character is intact
        compValue = user.getName().getFamilyName();
        compValue = compValue.substring(compValue.length() - 1);  //pick the last char
        assertEquals(unicodeStr, compValue);

    }

    @Test(dependsOnGroups = "lastTests", alwaysRun = true)
    public void delete() {
        deleteUser(user);
    }

}
