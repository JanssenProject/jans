package gluu.scim2.client.rest.provider;

import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;

import javax.ws.rs.Consumes;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.ext.Provider;

import static org.gluu.oxtrust.model.scim2.Constants.MEDIA_TYPE_SCIM_JSON;

@Provider
@Consumes({MEDIA_TYPE_SCIM_JSON, MediaType.APPLICATION_JSON})
@Produces({MEDIA_TYPE_SCIM_JSON, MediaType.APPLICATION_JSON})
public class ScimResourceProvider extends JacksonJsonProvider { }
