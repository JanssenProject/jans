package io.jans.scim2.client.patch;

import io.jans.scim.model.scim2.*;
import io.jans.scim.model.scim2.group.GroupResource;
import io.jans.scim.model.scim2.group.Member;
import io.jans.scim.model.scim2.patch.PatchOperation;
import io.jans.scim.model.scim2.patch.PatchRequest;
import io.jans.scim.model.scim2.user.UserResource;
import io.jans.scim.model.scim2.util.ScimResourceUtil;
import io.jans.scim2.client.BaseTest;

import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import jakarta.ws.rs.core.Response;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static jakarta.ws.rs.core.Response.Status.*;

import static org.testng.Assert.*;

public class PatchGroupTest extends BaseTest{

    private GroupResource group;
    private static final Class<GroupResource> groupCls=GroupResource.class;
    private static final Class<UserResource> usrClass=UserResource.class;

    @Parameters("group_minimal_create")
    @Test
    public void createGroup(String json){

        logger.debug("Creating group from json...");
        Response response = client.createGroup(json, null, null);
        assertEquals(response.getStatus(), CREATED.getStatusCode());

        group=response.readEntity(groupCls);
    }

    @Parameters("group_patch")
    @Test(dependsOnMethods = "createGroup")
    public void patch1(String jsonPatch){

        Response response=client.patchGroup(jsonPatch, group.getId(), null, null);
        assertEquals(response.getStatus(), OK.getStatusCode());

        GroupResource newerGroup=response.readEntity(groupCls);
        //Verify displayName changed
        assertNotEquals(newerGroup.getDisplayName(), group.getDisplayName());
        //Verify externalId appeared
        assertNotNull(newerGroup.getExternalId());

    }

    @Test(dependsOnMethods = "patch1")
    public void patch2() throws Exception{

        List<UserResource> users=getTestUsers("aaa");
        assertTrue(users.size()>0);

        //Define one "add" operation to insert the users retrieved in the created group
        PatchOperation operation=new PatchOperation();
        operation.setOperation("add");
        operation.setPath("members");

        List<Member> memberList=new ArrayList<>();
        users.stream().forEach(u -> {
            Member m=new Member();
            m.setType(ScimResourceUtil.getType(usrClass));
            m.setValue(u.getId());
            m.setDisplay(u.getDisplayName());
            m.setRef("/scim/v2/Users/" + u.getId());
            memberList.add(m);
        });

        operation.setValue(memberList);

        //Apply the patch to the group
        PatchRequest pr=new PatchRequest();
        pr.setOperations(Collections.singletonList(operation));

        Response response=client.patchGroup(pr, group.getId(), null, null);
        assertEquals(response.getStatus(), OK.getStatusCode());

        group=response.readEntity(groupCls);
        //Verify the new users are there
        Set<Member> members=group.getMembers();
        assertNotNull(members);
        assertTrue(members.stream()
                .allMatch(m -> m.getDisplay()!=null && m.getType()!=null && m.getValue()!=null && m.getRef()!=null));

        //Verify the Ids are the same (both provided and returned)
        Set<String> userIds=users.stream().map(UserResource::getId).collect(Collectors.toSet());
        assertTrue(members.stream().map(Member::getValue).collect(Collectors.toSet()).equals(userIds));

    }

    @Test(dependsOnMethods = "patch2")
    public void patch3(){

        Member members[]=group.getMembers().toArray(new Member[0]);

        //Try modifying one of the members. This should fail because of mutability
        PatchOperation operation=new PatchOperation();
        operation.setOperation("replace");
        operation.setPath(String.format("members[value eq \"%s\"].value", members[0].getValue()));
        operation.setValue(members[1].getValue());

        PatchRequest pr=new PatchRequest();
        pr.setOperations(Collections.singletonList(operation));

        Response response=client.patchGroup(pr, group.getId(), null, null);
        assertEquals(response.getStatus(), BAD_REQUEST.getStatusCode());

        //Try modifying one of the members. This should not fail ...
        operation.setValue(members[0].getValue());
        response=client.patchGroup(pr, group.getId(), null, null);
        assertEquals(response.getStatus(), OK.getStatusCode());

        //Try deleting value subattribute. This should fail ...
        operation.setOperation("remove");
        response=client.patchGroup(pr, group.getId(), null, null);
        assertEquals(response.getStatus(), BAD_REQUEST.getStatusCode());

        //Try removing one of the members. This should not fail ...
        operation.setPath(String.format("members[value eq \"%s\"]", members[0].getValue()));
        response=client.patchGroup(pr, group.getId(), null, null);
        group=response.readEntity(GroupResource.class);

        assertEquals(response.getStatus(), OK.getStatusCode());
        assertEquals(members.length-1, group.getMembers().size());

    }

    @Test(dependsOnMethods = "patch3", alwaysRun = true)
    public void delete(){
        Response response=client.deleteGroup(group.getId());
        assertEquals(response.getStatus(), NO_CONTENT.getStatusCode());
    }

    private List<UserResource> getTestUsers(String displayNamePattern){

        SearchRequest sr=new SearchRequest();
        sr.setFilter(String.format("displayName co \"%s\"", displayNamePattern));
        Response response=client.searchUsersPost(sr);
        ListResponse listResponse=response.readEntity(ListResponse.class);
        return listResponse.getResources().stream().map(usrClass::cast).collect(Collectors.toList());

    }

}
