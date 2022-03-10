/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.scim2.client.rest.provider;

import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;

import static io.jans.scim.model.scim2.Constants.MEDIA_TYPE_SCIM_JSON;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.ext.Provider;

@Provider
@Consumes({MEDIA_TYPE_SCIM_JSON, MediaType.APPLICATION_JSON})
@Produces({MEDIA_TYPE_SCIM_JSON, MediaType.APPLICATION_JSON})
public class ScimResourceProvider extends JacksonJsonProvider { }
