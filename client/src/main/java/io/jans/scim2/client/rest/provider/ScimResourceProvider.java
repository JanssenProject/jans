package io.jans.scim2.client.rest.provider;

import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;

import static io.jans.scim.model.scim2.Constants.MEDIA_TYPE_SCIM_JSON;

import javax.ws.rs.Consumes;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.ext.Provider;

@Provider
@Consumes({MEDIA_TYPE_SCIM_JSON, MediaType.APPLICATION_JSON})
@Produces({MEDIA_TYPE_SCIM_JSON, MediaType.APPLICATION_JSON})
public class ScimResourceProvider extends JacksonJsonProvider { }
