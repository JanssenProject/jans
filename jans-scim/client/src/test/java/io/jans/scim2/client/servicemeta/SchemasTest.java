package io.jans.scim2.client.servicemeta;

import io.jans.scim.model.scim2.BaseScimResource;
import io.jans.scim.model.scim2.ListResponse;
import io.jans.scim.model.scim2.fido.FidoDeviceResource;
import io.jans.scim.model.scim2.fido.Fido2DeviceResource;
import io.jans.scim.model.scim2.group.GroupResource;
import io.jans.scim.model.scim2.provider.schema.SchemaAttribute;
import io.jans.scim.model.scim2.provider.schema.SchemaResource;
import io.jans.scim.model.scim2.user.UserResource;
import io.jans.scim.model.scim2.util.ScimResourceUtil;
import io.jans.scim2.client.BaseTest;

import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import jakarta.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static io.jans.scim.model.scim2.Constants.USER_EXT_SCHEMA_ID;
import static org.testng.Assert.*;

public class SchemasTest extends BaseTest {

    private ListResponse listResponse;

    @BeforeTest
    public void init() throws Exception {
        Response response = client.getSchemas();
        listResponse = response.readEntity(ListResponse.class);
        assertTrue(listResponse.getTotalResults() > 0);
    }

    @Test
    public void checkSchemasExistence() {

        List<String> schemas = new ArrayList<>();
        schemas.add(USER_EXT_SCHEMA_ID);

        List<Class<? extends BaseScimResource>> classes = Arrays.asList(UserResource.class, GroupResource.class, FidoDeviceResource.class, Fido2DeviceResource.class);
        classes.forEach(cls -> schemas.add(ScimResourceUtil.getSchemaAnnotation(cls).id()));
        //Verifies default schemas for the 3 main SCIM resources + user extension are part of /Schemas endpoint
        listResponse.getResources().forEach(res -> assertTrue(schemas.contains(res.getId())));

    }

    @Test(dependsOnMethods = "checkSchemasExistence")
    public void inspectUserExtensionSchema(){

        Optional<SchemaResource> userExtSchemaOpt=listResponse.getResources().stream().map(SchemaResource.class::cast)
                .filter(sr -> sr.getId().contains(USER_EXT_SCHEMA_ID)).findFirst();

        if (userExtSchemaOpt.isPresent()){
            String name=userExtSchemaOpt.get().getName();
            assertNotNull(name);
            logger.info("Found User Schema Extension: {}", name);

            Boolean foundAttr[]=new Boolean[3];
            for (SchemaAttribute attribute : userExtSchemaOpt.get().getAttributes()){
                switch (attribute.getName()) {
                    case "scimCustomFirst":
                        foundAttr[0] = true;
                        logger.info("scimCustomFirst found");
                        assertEquals(attribute.getType(),"string");
                        assertFalse(attribute.isMultiValued());
                        break;
                    case "scimCustomSecond":
                        foundAttr[1] = true;
                        logger.info("scimCustomSecond found");
                        assertEquals(attribute.getType(),"dateTime");
                        assertTrue(attribute.isMultiValued());
                        break;
                    case "scimCustomThird":
                        foundAttr[2] = true;
                        logger.info("scimCustomThird found");
                        assertEquals(attribute.getType(),"decimal");
                        assertFalse(attribute.isMultiValued());
                        break;
                }
            }
            Arrays.asList(foundAttr).forEach(org.testng.Assert::assertTrue);

        }
        else
            logger.warn("There is no Schema Resource associated to User Schema Extension ({})", USER_EXT_SCHEMA_ID);

    }

}
