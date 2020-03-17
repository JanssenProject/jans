package gluu.scim2.client.singleresource;

import gluu.scim2.client.BaseTest;
import org.gluu.oxtrust.model.scim2.group.GroupResource;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import javax.ws.rs.core.Response;

import static javax.ws.rs.core.Response.Status.*;

import static org.testng.Assert.*;

/**
 * Created by jgomer on 2017-10-21.
 */
public class MinimalGroupTest extends BaseTest {

    private GroupResource group;
    private static final Class<GroupResource> groupClass=GroupResource.class;

    @Parameters("group_minimal_create")
    @Test
    public void createGroup(String json){

        logger.debug("Creating mimimal group from json...");
        Response response = client.createGroup(json, null, null);
        assertEquals(response.getStatus(), CREATED.getStatusCode());

        group=response.readEntity(groupClass);
        assertNotNull(group.getMeta());
        logger.debug("Group created with id {}", group.getId());

    }

    @Parameters("group_minimal_update")
    @Test(dependsOnMethods="createGroup")
    public void updateGroup(String json){

        logger.debug("Updating group {} with json", group.getDisplayName());
        Response response=client.updateGroup(json, group.getId(), null, null);
        assertEquals(response.getStatus(), OK.getStatusCode());

        GroupResource updated=response.readEntity(groupClass);
        assertNotEquals(group.getDisplayName(), updated.getDisplayName());
        assertNull(updated.getExternalId());     //Impl. ignores externalId for groups
        logger.debug("Updated group {}", updated.getDisplayName());

    }

    @Test(dependsOnMethods="updateGroup", alwaysRun = true)
    public void delete(){

        logger.debug("Deleting group {}", group.getDisplayName());
        Response response=client.deleteGroup(group.getId());
        assertEquals(response.getStatus(), NO_CONTENT.getStatusCode());
        logger.debug("deleted");

    }

}
