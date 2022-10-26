package io.jans.scim2.client.singleresource;

import org.apache.commons.beanutils.BeanUtils;
import io.jans.scim.model.scim2.ListResponse;
import io.jans.scim.model.scim2.fido.Fido2DeviceResource;
import io.jans.scim.model.scim2.util.IntrospectUtil;
import io.jans.scim2.client.BaseTest;

import org.testng.annotations.Test;

import jakarta.ws.rs.core.Response;

import static jakarta.ws.rs.core.Response.Status.BAD_REQUEST;
import static jakarta.ws.rs.core.Response.Status.NO_CONTENT;
import static jakarta.ws.rs.core.Response.Status.OK;
import static org.testng.Assert.*;

/**
 * NOTES:
 * Ensure the fido2 service is up and running
 */
public class Fido2DeviceTest extends BaseTest {

    private Fido2DeviceResource device;
    private static final Class<Fido2DeviceResource> fido2Class=Fido2DeviceResource.class;

    @Test
    public void search(){

        logger.debug("Searching all fido 2 devices");
        Response response=client.searchF2Devices(null, "id pr", null, null, null, null, null, null);
        assertEquals(response.getStatus(), OK.getStatusCode());

        ListResponse listResponse=response.readEntity(ListResponse.class);
        //Work upon the first device of the list only
        device=(Fido2DeviceResource) listResponse.getResources().get(0);
        assertNotNull(device);
        logger.debug("First device {} picked", device.getId());

    }

    @Test(dependsOnMethods = "search")
    public void retrieve(){

        logger.debug("Retrieving same device by id");
        Response response=client.getF2DeviceById(device.getId(), device.getUserId(), null, null);
        assertEquals(response.getStatus(), OK.getStatusCode());

        Fido2DeviceResource same=response.readEntity(fido2Class);
        assertEquals(device.getId(), same.getId());
        assertEquals(device.getStatus(), same.getStatus());
        assertEquals(device.getCounter(), same.getCounter());
        assertEquals(device.getCreationDate(), same.getCreationDate());
    }

    @Test(dependsOnMethods = "retrieve")
    public void updateWithJson() throws Exception{

        //shallow clone device
        Fido2DeviceResource clone=(Fido2DeviceResource) BeanUtils.cloneBean(device);
        String name = "The quick brown fox jumps over the lazy dog";

        clone.setDisplayName(name);
        String json=mapper.writeValueAsString(clone);

        logger.debug("Updating device with json");
        Response response=client.updateF2Device(json, device.getId(), null, null);
        assertEquals(response.getStatus(), OK.getStatusCode());

        Fido2DeviceResource updated=response.readEntity(fido2Class);
        assertNotEquals(updated.getDisplayName(), device.getDisplayName());
        assertEquals(updated.getDisplayName(), name);

    }

    @Test(dependsOnMethods = "updateWithJson")
    public void updateWithObject() throws Exception{

        logger.debug("Updating device to original attributes");
        Response response=client.updateF2Device(device, device.getId(), null, null);
        assertEquals(response.getStatus(), OK.getStatusCode());

        Fido2DeviceResource updated=response.readEntity(fido2Class);

        //Naively compare (property-to-property) the original and new object. It's feasible since all of them are strings
        for (String path : IntrospectUtil.allAttrs.get(fido2Class)){
            String val=BeanUtils.getProperty(device, path);
            //Exclude metas since they diverge and skip if original attribute was null (when passing null for an update, server ignores)
            if (!path.startsWith("meta") && val!=null) {
                assertEquals(BeanUtils.getProperty(updated, path), val);
            }
        }

        //Update an immutable attribute
        updated.setCounter(Integer.MIN_VALUE);
        response=client.updateF2Device(updated, updated.getId(), null, null);
        assertEquals(response.getStatus(), BAD_REQUEST.getStatusCode());

    }

    //@Test(dependsOnMethods = "updateWithObject", alwaysRun = true)
    public void delete(){
        logger.debug("Deleting Fido 2 device");
        Response response=client.deleteF2Device(device.getId());
        assertEquals(response.getStatus(), NO_CONTENT.getStatusCode());
        logger.debug("deleted");
    }

}
