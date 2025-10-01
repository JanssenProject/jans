package io.jans.scim2.client.search;

import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.beanutils.NestedNullException;
import io.jans.scim.model.scim2.BaseScimResource;
import io.jans.scim.model.scim2.ListResponse;
import io.jans.scim.model.scim2.SearchRequest;
import io.jans.scim.model.scim2.user.UserResource;
import io.jans.scim.model.scim2.util.IntrospectUtil;
import io.jans.scim2.client.UserBaseTest;

import org.testng.annotations.Test;

import jakarta.ws.rs.core.Response;

import java.time.ZonedDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static jakarta.ws.rs.core.Response.Status.*;
import static org.testng.Assert.*;

public class ComplexSearchUserTest extends UserBaseTest {

    @Test
    public void searchNoAttributesParam(){

        final String ims="Skype";
        logger.debug("Searching users with attribute nickName existent or ims.value={} using POST verb", ims);

        SearchRequest sr=new SearchRequest();
        sr.setFilter("nickName pr or ims.value eq \"" + ims + "\"");
        Response response=client.searchUsersPost(sr);
        assertEquals(response.getStatus(), OK.getStatusCode());

        ListResponse listResponse=response.readEntity(ListResponse.class);
        if (listResponse.getResources() != null) {

            for (BaseScimResource resource : listResponse.getResources()) {
                UserResource other = (UserResource) resource;

                boolean c1 = other.getNickName() != null;
                boolean c2 = true;
                if (other.getIms() != null)
                    c2 = other.getIms().stream().anyMatch(im -> im.getValue().toLowerCase().equals(ims.toLowerCase()));

                assertTrue(c1 || c2);
            }
        }

    }

    @Test
    public void searchAttributesParam() throws Exception {

        int count=3;
        List<String> attrList=Arrays.asList("name.familyName", "active");
        logger.debug("Searching at most {} users using POST verb", count);
        logger.debug("Sorted by family name descending");
        logger.debug("Retrieving only the attributes {}", attrList);

        SearchRequest sr=new SearchRequest();
        sr.setFilter("name.familyName pr");
        sr.setSortBy("name.familyName");
        sr.setSortOrder("descending");
        //Generate a string with the attributes desired to be returned separated by comma
        sr.setAttributes(attrList.toString().replaceFirst("\\[","").replaceFirst("]",""));
        sr.setCount(count);

        Response response=client.searchUsersPost(sr);
        assertEquals(response.getStatus(), OK.getStatusCode());

        ListResponse listResponse=response.readEntity(ListResponse.class);
        if (listResponse.getResources().size()<count)
            logger.warn("Less than {} users satisfying the criteria. TESTER please check manually", count);
        else{
            //Obtain an array of results
            UserResource users[]=listResponse.getResources().stream().map(usrClass::cast)
                    .collect(Collectors.toList()).toArray(new UserResource[0]);
            assertEquals(users.length, count);

            //Build a set of all attributes that should not appear in the response
            Set<String> check=new HashSet<>();
            check.addAll(IntrospectUtil.allAttrs.get(usrClass));

            //Remove from the ALL list, those requested plus its "parents"
            for (String attr : attrList){
                String part=attr;

                for (int i=part.length(); i>0; i = part.lastIndexOf(".")){
                    part=part.substring(0, i);
                    check.remove(part);
                }
            }
            //Remove those that are ALWAYS present (per spec)
            check.removeAll(IntrospectUtil.alwaysCoreAttrs.get(usrClass).keySet());

            //Confirm for every user, those attributes are not there
            for (UserResource user : users) {
                for (String path : check) {
                    String val = null;
                    try {
                        val = BeanUtils.getProperty(user, path);
                    } catch (NestedNullException nne) {
                        //Intentionally left empty
                    } finally {
                        assertNull(val);
                    }
                }
            }

            boolean correctSorting = true;
            for (int i=1;i<users.length && correctSorting;i++) {
                String familyName=users[i-1].getName().getFamilyName();
                String familyName2=users[i].getName().getFamilyName();

                //First string has to be greater than or equal second
                correctSorting = familyName.compareTo(familyName2)>=0;
            }

            if (!correctSorting) {
                //LDAP may ignore case sensitivity, try again using lowercasing
                correctSorting = true;
                for (int i=1;i<users.length && correctSorting;i++) {
                    String familyName=users[i-1].getName().getFamilyName().toLowerCase();
                    String familyName2=users[i].getName().getFamilyName().toLowerCase();

                    //First string has to be greater than or equal second
                    correctSorting = familyName.compareTo(familyName2)>=0;
                }
            }
            assertTrue(correctSorting);

        }

    }

    @Test
    public void searchExcludedAttributesParam() {

        int count=3;
        List<String> attrList=Arrays.asList("x509Certificates", "entitlements", "roles", "ims", "phoneNumbers",
                "addresses", "emails", "groups");
        logger.debug("Searching at most {} users using POST verb", count);
        logger.debug("Sorted by displayName ascending");
        logger.debug("Excluding the attributes {}", attrList);

        SearchRequest sr=new SearchRequest();
        sr.setFilter("displayName pr");
        sr.setSortBy("displayName");
        sr.setSortOrder("descending");
        //Generate a string with the attributes to exclude
        sr.setExcludedAttributes(attrList.toString().replaceFirst("\\[","").replaceFirst("]",""));
        sr.setCount(count);

        Response response=client.searchUsersPost(sr);
        assertEquals(response.getStatus(), OK.getStatusCode());

        ListResponse listResponse=response.readEntity(ListResponse.class);
        if (listResponse.getResources().size()<count)
            logger.warn("Less than {} users satisfying the criteria. TESTER please check manually", count);
        else {
            //Obtain an array of results
            UserResource users[] = listResponse.getResources().stream().map(usrClass::cast)
                    .collect(Collectors.toList()).toArray(new UserResource[0]);
            assertEquals(users.length, count);

            //Verify attributes were excluded
            for (UserResource u : users){
                assertNull(u.getX509Certificates());
                assertNull(u.getEntitlements());
                assertNull(u.getRoles());
                assertNull(u.getIms());
                assertNull(u.getPhoneNumbers());
                assertNull(u.getAddresses());
                assertNull(u.getEmails());
            }

            boolean correctSorting = true;
            for (int i=1;i<users.length && correctSorting;i++) {
                String displayName=users[i-1].getDisplayName();
                String displayName2 =users[i].getDisplayName();

                //Check if second string is less or equal than first
                correctSorting = displayName.compareTo(displayName2)>=0;
            }

            if (!correctSorting) {
                //LDAP may ignore case sensitivity, try again using lowercasing
                correctSorting = true;
                for (int i=1;i<users.length && correctSorting;i++) {
                    String displayName=users[i-1].getDisplayName().toLowerCase();
                    String displayName2 =users[i].getDisplayName().toLowerCase();

                    //Check if second string is less or equal than first
                    correctSorting = displayName.compareTo(displayName2)>=0;
                }
            }
            assertTrue(correctSorting);
        }
    }

    //This test is disabled to avoid problems in attribute excludeMetaLastMod being inconsistent with updatedAt in testing server
    //@Test
    public void searchSortByDate() {

        SearchRequest sr=new SearchRequest();
        sr.setFilter("userName pr");
        sr.setSortBy("meta.lastModified");
        sr.setAttributes(Collections.singletonList(sr.getSortBy()));

        Response response=client.searchUsersPost(sr);
        assertEquals(response.getStatus(), OK.getStatusCode());

        ListResponse listResponse=response.readEntity(ListResponse.class);
        UserResource users[] = listResponse.getResources().stream().map(usrClass::cast).collect(Collectors.toList())
                .toArray(new UserResource[0]);

        for (int i=1; i<users.length; i++) {
            String lastMod1=users[i-1].getMeta()==null ? null : users[i-1].getMeta().getLastModified();
            String lastMod2=users[i].getMeta()==null ? null : users[i].getMeta().getLastModified();

            if (lastMod1!=null)     //Both being non null is OK
                assertNotNull(lastMod2);
            if (lastMod2==null)     //If second is null, first must be
                assertNull(lastMod1);
            if (lastMod1!=null && lastMod2!=null) {
                ZonedDateTime dt1=ZonedDateTime.parse(lastMod1);
                ZonedDateTime dt2=ZonedDateTime.parse(lastMod2);
                assertTrue(dt1.isEqual(dt2) || dt1.isBefore(dt2));
            }
        }

    }

    @Test
    public void searchSortByExternalId() {

        Response response=client.searchUsers(null, null, null, "externalId", "descending", "externalId", null);
        assertEquals(response.getStatus(), OK.getStatusCode());

        ListResponse listResponse=response.readEntity(ListResponse.class);
        UserResource users[] = listResponse.getResources().stream().map(usrClass::cast).collect(Collectors.toList())
                .toArray(new UserResource[0]);

        assertEquals(listResponse.getStartIndex(), 1);
        assertEquals(listResponse.getItemsPerPage(), users.length);
        assertEquals(listResponse.getResources().size(), users.length);

        for (int i=1; i<users.length; i++) {
            String exId1=users[i-1].getExternalId();
            String exId2=users[i].getExternalId();

            if (exId1!=null && exId2!=null)     //In descending order exId1 must be higher than exId2
                assertFalse(exId1.compareTo(exId2)<0);
        }

    }

}
