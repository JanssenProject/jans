package io.jans.as.server.ws.rs.org;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.jboss.resteasy.annotations.providers.jaxb.IgnoreMediaTypes;

import java.util.HashMap;
import java.util.Map;

@IgnoreMediaTypes("application/*+json")
@JsonIgnoreProperties(ignoreUnknown = true)
public class OrganizationConfigResponse {

}