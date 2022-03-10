/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.scim2.client.multipleresource;

import io.jans.scim.model.scim2.BaseScimResource;
import io.jans.scim.model.scim2.ListResponse;
import io.jans.scim.model.scim2.SearchRequest;
import io.jans.scim.model.scim2.group.GroupResource;
import io.jans.scim.model.scim2.group.Member;
import io.jans.scim.model.scim2.user.Group;
import io.jans.scim.model.scim2.user.UserResource;
import io.jans.scim2.client.UserBaseTest;

import org.testng.annotations.Test;

import jakarta.ws.rs.core.Response;
import java.util.*;
import java.util.stream.Collectors;

import static jakarta.ws.rs.core.Response.Status.*;

import static org.testng.Assert.*;

/**
 * This test is quite representative of SCIM funtionalities: it covers a good amount of operations in different flavors,
 * and at the same time showcases how User's group assignment works
 * Created by jgomer on 2017-12-28.
 */
public class GroupAssignUserTest extends UserBaseTest {

    private List<UserResource> friends=new ArrayList<>();
    private GroupResource group, group2;
    private UserResource user;

    @Test
    public void createUsers(){

        logger.info("Creating 3 users...");
        List<UserResource> mentals =new ArrayList<>();
        //Hugo, Paco, and Luis; or Curly, and the other 2 crazy men
        Arrays.asList(1,2,3).forEach( who -> mentals.add(getDummyPatient()));
        for (UserResource user : mentals){
            Response response=client.createUser(user, null, null);
            assertEquals(response.getStatus(), CREATED.getStatusCode());
            friends.add(response.readEntity(usrClass));
        }

    }

    @Test(dependsOnMethods = "createUsers")
    public void assignToGroup(){

        Set<Member> buddies=friends.stream().map(buddy -> {
            Member m=new Member();
            m.setValue(buddy.getId());
            m.setDisplay(buddy.getDisplayName());
            return m;
        }).collect(Collectors.toSet());

        GroupResource gr=new GroupResource();
        gr.setDisplayName("3 best demented buddies");
        gr.setMembers(buddies);

        logger.info("Assigning users to new group...");
        Response response=client.createGroup(gr, null, null);
        assertEquals(response.getStatus(), CREATED.getStatusCode());
        group=response.readEntity(GroupResource.class);

        //Verify the sanitarium is completely booked
        assertTrue(
            group.getMembers().stream().map(Member::getValue).collect(Collectors.toSet()).equals(
                    friends.stream().map(UserResource::getId).collect(Collectors.toSet())
        ));

    }

    @Test(dependsOnMethods = "assignToGroup")
    public void assignToSecondGroup(){

        //Creates a new group with only the first patient on it
        Member m=new Member();
        m.setValue(friends.get(0).getId());

        group2=new GroupResource();
        group2.setDisplayName("Auxiliary asylum");
        group2.setMembers(Collections.singleton(m));

        logger.info("Creating a secondary group...");
        Response response=client.createGroup(group2, null, null);
        assertEquals(response.getStatus(), CREATED.getStatusCode());
        group2=response.readEntity(GroupResource.class);

    }

    @Test(dependsOnMethods = "assignToSecondGroup")
    public void verifyGroupsAttribute(){

        //Refresh the user instances so getGroups() can be called

        //builds a filter string
        StringBuilder filter=new StringBuilder();
        friends.forEach(buddy -> filter.append(String.format(" or id eq \"%s\"", buddy.getId())));

        //builds a search request
        SearchRequest sr=new SearchRequest();
        sr.setFilter(filter.substring(4));
        sr.setCount(3);     //Retrieve only the first 3

        //Performs the query
        logger.info("Issuing query with filter: {}", sr.getFilter());
        Response response=client.searchUsersPost(sr);
        assertEquals(response.getStatus(), OK.getStatusCode());

        logger.info("Verifying groups and users consistency...");
        List<BaseScimResource> buddies=response.readEntity(ListResponse.class).getResources();
        assertEquals(buddies.size(),3);

        //Verify all mad belong to group, and one of them, additionally to group2
        buddies.stream().map(usrClass::cast).forEach(buddy -> {
            Set<String> groupIds=buddy.getGroups().stream().map(Group::getValue).collect(Collectors.toSet());
            assertTrue(groupIds.contains(group.getId()));
        });

        Optional<UserResource> usrOpt=buddies.stream().map(usrClass::cast)
                .filter(buddy -> buddy.getGroups().size()>1).findFirst();
        assertTrue(usrOpt.isPresent());

        user=usrOpt.get();
        assertTrue(user.getGroups().stream().map(Group::getValue).collect(Collectors.toSet()).contains(group2.getId()));

    }

    @Test(dependsOnMethods = "verifyGroupsAttribute")
    public void modifyGroupFromUser(){

        //Try to modify read-only "groups" attribute of User Resource (must not change)
        user.getGroups().remove(0);
        Response response=client.updateUser(user, user.getId(), null, null);
        assertEquals(response.getStatus(), OK.getStatusCode());

        logger.info("Attempting to modify group membership using /Users endpoint...");
        user=response.readEntity(usrClass);
        Set<String> groupIds=user.getGroups().stream().map(Group::getValue).collect(Collectors.toSet());

        assertTrue(groupIds.contains(group.getId()));
        assertTrue(groupIds.contains(group2.getId()));

    }

    @Test(dependsOnMethods = "modifyGroupFromUser")
    public void alterMemberships(){

        //Effectively remove one member and add admin
        Member aMental=group.getMembers().stream().findAny().get();
        Member admin=new Member();
        admin.setValue(getAdminId());

        group.getMembers().remove(aMental);
        group.getMembers().add(admin);

        logger.info("Removing one and adding one member...");
        Response response=client.updateGroup(group, group.getId(), null, null);

        assertEquals(response.getStatus(), OK.getStatusCode());
        group=response.readEntity(GroupResource.class);

        assertFalse(group.getMembers().contains(aMental));
        //Here we don't use contains because equality in Member object inspects all fields (not only value)
        assertTrue(group.getMembers().stream().anyMatch(m -> admin.getValue().equals(m.getValue())));
        logger.info("Group has correct members");

        //Verify groups attribute in users reflected changes
        response=client.getUserById(aMental.getValue(), "groups", null);
        assertEquals(response.getStatus(), OK.getStatusCode());

        UserResource patient=response.readEntity(usrClass);
        assertTrue(patient.getGroups()==null || patient.getGroups().stream().noneMatch(gr -> gr.getValue().equals(group.getId())));

        response=client.getUserById(admin.getValue(), "groups", null);
        assertEquals(response.getStatus(), OK.getStatusCode());

        patient=response.readEntity(usrClass);
        assertTrue(patient.getGroups().stream().anyMatch(gr -> gr.getValue().equals(group.getId())));
        logger.info("Users have correct memberships");

    }


    @Test(dependsOnMethods = "alterMemberships", alwaysRun = true)
    public void deleteGroups(){

        //Dismantle sanitarium...
        for (GroupResource gr : Arrays.asList(group, group2))
            if (gr!=null){
                Response response=client.deleteGroup(gr.getId());
                if (response.getStatus()==NO_CONTENT.getStatusCode())
                    logger.info("Group '{}' removed", gr.getDisplayName());
                else
                    logger.error("Error removing group '{}'", gr.getDisplayName());
            }

    }

    @Test(dependsOnMethods = "deleteGroups", alwaysRun = true)
    public void deleteUsers(){

        if (friends!=null){
            //Considered sane now
            for (UserResource usr : friends){
                Response response=client.deleteUser(usr.getId());
                if (response.getStatus()==NO_CONTENT.getStatusCode())
                    logger.info("User '{}' removed", usr.getDisplayName());
                else
                    logger.error("Error removing user '{}'", usr.getDisplayName());
            }
        }

    }

    private UserResource getDummyPatient() {

        UserResource user = new UserResource();
        user.setUserName("test-" + Math.random());
        user.setDisplayName(user.getUserName());
        return user;
    }

    private String getAdminId(){

        //Search the id of the admin user
        SearchRequest sr=new SearchRequest();
        sr.setFilter("userName eq \"admin\"");

        Response response=client.searchUsersPost(sr);
        assertEquals(response.getStatus(), OK.getStatusCode());

        ListResponse lr=response.readEntity(ListResponse.class);
        assertTrue(lr.getResources().size()>0);
        return lr.getResources().get(0).getId();

    }

}
