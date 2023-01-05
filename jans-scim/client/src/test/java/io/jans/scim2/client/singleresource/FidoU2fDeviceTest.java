package io.jans.scim2.client.singleresource;

import org.apache.commons.beanutils.BeanUtils;
import io.jans.scim.model.scim2.ListResponse;
import io.jans.scim.model.scim2.fido.FidoDeviceResource;
import io.jans.scim.model.scim2.util.IntrospectUtil;
import io.jans.scim2.client.BaseTest;

import org.testng.annotations.Test;

import java.util.Optional;
import jakarta.ws.rs.core.Response;

import static jakarta.ws.rs.core.Response.Status.*;

import static org.testng.Assert.*;

public class FidoU2fDeviceTest extends BaseTest {

    private FidoDeviceResource device;
    private static final Class<FidoDeviceResource> fidoClass=FidoDeviceResource.class;

    @Test
    public void search() {

        logger.debug("Searching all fido u2f devices");
        Response response=client.searchDevices(null, "application pr", null, null, null, null, null, null);
        assertEquals(response.getStatus(), OK.getStatusCode());

        ListResponse listResponse=response.readEntity(ListResponse.class);
        
        //Work upon the first device whose deviceData is empty
        Optional<FidoDeviceResource> opt = listResponse.getResources().stream()
                                               .map(FidoDeviceResource.class::cast)
                                               .filter(dev -> dev.getDeviceData() == null).findAny();	
        
        assertTrue(opt.isPresent());        
        device = opt.get();
        logger.debug("First device {} picked", device.getId());

    }

    @Test(dependsOnMethods = "search")
    public void retrieve(){

        logger.debug("Retrieving same device by id");
        Response response=client.getDeviceById(device.getId(), device.getUserId(), null, null);
        assertEquals(response.getStatus(), OK.getStatusCode());

        FidoDeviceResource same=response.readEntity(fidoClass);
        assertEquals(device.getId(), same.getId());
        assertEquals(device.getApplication(), same.getApplication());
        assertEquals(device.getCounter(), same.getCounter());
        assertEquals(device.getDeviceKeyHandle(), same.getDeviceKeyHandle());
    }

    @Test(dependsOnMethods = "retrieve")
    public void updateWithJson() throws Exception{

        //shallow clone device
        FidoDeviceResource clone=(FidoDeviceResource) BeanUtils.cloneBean(device);

        clone.setDisplayName(Double.toString(Math.random()));
        clone.setNickname("compromised");
        String json=mapper.writeValueAsString(clone);

        logger.debug("Updating device with json");
        Response response=client.updateDevice(json, device.getId(), null, null);
        assertEquals(response.getStatus(), OK.getStatusCode());

        FidoDeviceResource updated=response.readEntity(fidoClass);
        assertNotEquals(updated.getDisplayName(), device.getDisplayName());
        assertEquals(updated.getNickname(), "compromised");

    }

    @Test(dependsOnMethods = "updateWithJson")
    public void updateWithObject() throws Exception{

        logger.debug("Updating device to original attributes");
        Response response=client.updateDevice(device, device.getId(), null, null);
        assertEquals(response.getStatus(), OK.getStatusCode());

        FidoDeviceResource updated=response.readEntity(fidoClass);

        //Naively compare (property-to-property) the original and new object. It's feasible since all of them are strings
        for (String path : IntrospectUtil.allAttrs.get(fidoClass)){
            String val=BeanUtils.getProperty(device, path);
            //Exclude metas since they diverge and skip if original attribute was null (when passing null for an update, server ignores)
            if (!path.startsWith("meta") && val!=null)
                assertEquals(BeanUtils.getProperty(updated, path), val);
        }

        //Update an immutable attribute (originally null). Per spec, uninitialized immutable attributes can be set
        assertNull(updated.getDeviceData());
        updated.setDeviceData("Dummy device data");
        response=client.updateDevice(updated, updated.getId(), null, null);
        assertEquals(response.getStatus(), OK.getStatusCode());

        updated=response.readEntity(fidoClass);
        assertNotNull(updated.getDeviceData());

        //NOTE: if you don't see device data attribute for this device in LDAP is because the attribute is marked as being
        //ignored upon update (see io.jans.scim.model.fido.GluuCustomFidoDevice)

    }

    //@Test(dependsOnMethods = "updateWithObject", alwaysRun = true)
    public void delete(){
        logger.debug("Deleting device");
        Response response=client.deleteDevice(device.getId());
        assertEquals(response.getStatus(), NO_CONTENT.getStatusCode());
        logger.debug("deleted");
    }

}
