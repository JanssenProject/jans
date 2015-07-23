/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.idgen.ws.rs;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.commons.lang.StringUtils;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Logger;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.log.Log;
import org.xdi.oxauth.model.common.Id;
import org.xdi.oxauth.model.common.IdType;
import org.xdi.oxauth.model.common.uma.UmaRPT;
import org.xdi.oxauth.model.config.ConfigurationFactory;
import org.xdi.oxauth.model.uma.persistence.ResourceSetPermission;
import org.xdi.oxauth.service.token.TokenService;
import org.xdi.oxauth.service.uma.RPTManager;
import org.xdi.oxauth.service.uma.resourceserver.PermissionService;
import org.xdi.oxauth.service.uma.resourceserver.RsResourceType;
import org.xdi.oxauth.service.uma.resourceserver.RsScopeType;
import org.xdi.oxauth.util.ServerUtil;
import org.xdi.util.Pair;

import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiParam;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 24/06/2013
 */

@Name("idGenWS")
@Path("/id")
@Api(value = "/id", description = "ID Generation")
public class IdGenRestWebService {

    private static class UnauthorizedResponseHolder {
        public static Response UNAUTHORIZED_RESPONSE = unauthorizedResponse();

        public static Response unauthorizedResponse() {
            return Response.status(Response.Status.UNAUTHORIZED).
                    header("host_id", ConfigurationFactory.instance().getConfiguration().getIssuer()).
                    header("as_uri", ConfigurationFactory.instance().getConfiguration().getUmaConfigurationEndpoint()).
                    build();
        }

    }

    @Logger
    private Log log;
    @In
    private IdGenService idGenService;
    @In
    private TokenService tokenService;
    @In
    private PermissionService umaRsPermissionService;
    @In
    private RPTManager rptManager;

    @GET
    @Path("/{prefix}/{type}/")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(
            value = "Generates ID for given prefix and type.",
            notes = "Generates ID for given prefix and type. ",
            response = Response.class,
            responseContainer = "String"
    )
    public Response generateJsonInum(
            @PathParam("prefix") @ApiParam(value="Prefix for id. E.g. if prefix is @!1111 and server will generate id: !0000 then ID returned by service would be: @!1111!0000", required = true)
            String prefix,
            @PathParam("type") @ApiParam(value="Type of id", required = true, allowableValues = "PEOPLE, ORGANIZATION, APPLIANCE, GROUP, SERVER, ATTRIBUTE, TRUST_RELATIONSHIP, CLIENTS")
            String type,
            @HeaderParam("Authorization") String p_authorization) {
        return generateId(prefix, type, p_authorization, MediaType.APPLICATION_JSON);
    }

    @GET
    @Path("/{prefix}/{type}/")
    @Produces(MediaType.TEXT_PLAIN)
    @ApiOperation(
            value = "Generates ID for given prefix and type.",
            notes = "Generates ID for given prefix and type. ",
            response = Response.class,
            responseContainer = "String"
    )
    public Response generateTextInum(@PathParam("prefix") String prefix, @PathParam("type") String type, @HeaderParam("Authorization") String p_authorization) {
        return generateId(prefix, type, p_authorization, MediaType.TEXT_PLAIN);
    }

    @GET
    @Path("/{prefix}/{type}/")
    @Produces(MediaType.TEXT_XML)
    @ApiOperation(
            value = "Generates ID for given prefix and type.",
            notes = "Generates ID for given prefix and type. ",
            response = Response.class,
            responseContainer = "String"
    )
    public Response generateXmlInum(
            @PathParam("prefix") @ApiParam(value="Prefix for id. E.g. if prefix is @!1111 and server will generate id: !0000 then ID returned by service would be: @!1111!0000", required = true)
            String prefix,
            @PathParam("type") @ApiParam(value="Type of id", required = true, allowableValues = "PEOPLE, ORGANIZATION, APPLIANCE, GROUP, SERVER, ATTRIBUTE, TRUST_RELATIONSHIP, CLIENTS")
            String type,
            @HeaderParam("Authorization") String p_authorization) {
        return generateId(prefix, type, p_authorization, MediaType.TEXT_XML);
    }

    @GET
    @Path("/{prefix}/{type}/")
    @Produces(MediaType.TEXT_HTML)
    @ApiOperation(
            value = "Generates ID for given prefix and type.",
            notes = "Generates ID for given prefix and type. ",
            response = Response.class,
            responseContainer = "String"
    )
    public Response generateHtmlInum(
            @PathParam("prefix") @ApiParam(value="Prefix for id. E.g. if prefix is @!1111 and server will generate id: !0000 then ID returned by service would be: @!1111!0000", required = true)
            String prefix,
            @PathParam("type") @ApiParam(value="Type of id", required = true, allowableValues = "PEOPLE, ORGANIZATION, APPLIANCE, GROUP, SERVER, ATTRIBUTE, TRUST_RELATIONSHIP, CLIENTS")
            String type,
            @HeaderParam("Authorization") String p_authorization) {
        return generateId(prefix, type, p_authorization, MediaType.TEXT_HTML);
    }

    private Pair<Boolean, Response> hasEnoughPermissions(String p_authorization, List<RsScopeType> p_scopes) {
        final String rptString = tokenService.getTokenFromAuthorizationParameter(p_authorization);
        if (StringUtils.isNotBlank(rptString)) {
            final UmaRPT rpt = rptManager.getRPTByCode(rptString);
            if (rpt != null) {
                rpt.checkExpired();
                if (rpt.isValid()) {
                    final List<ResourceSetPermission> rptPermissions = rptManager.getRptPermissions(rpt);
                    return umaRsPermissionService.hasEnoughPermissionsWithTicketRegistration(rpt, rptPermissions, RsResourceType.ID_GENERATION, p_scopes);
                }
            }
        }

        // If the client does not present an RPT with the request,
        // the resource server MUST return an HTTP 401 (Unauthorized) status code,
        // along with providing the authorization server's URI in an "as_uri" property
        // to facilitate authorization server configuration data discovery,
        // including discovery of the endpoint where the client can request an RPT (Section 3.4.1).
        log.debug("Client does not present RPT. Return HTTP 401 (Unauthorized)\n with reference to AM as_uri: {0}",
                ConfigurationFactory.instance().getConfiguration().getUmaConfigurationEndpoint());

        return new Pair<Boolean, Response>(false, UnauthorizedResponseHolder.UNAUTHORIZED_RESPONSE);
    }

    private Response generateId(String prefix, String type, String p_authorization, String p_mediaType) {
        try {
            final Pair<Boolean, Response> pair = hasEnoughPermissions(p_authorization, Arrays.asList(RsScopeType.GENERATE_ID));
            if (pair.getFirst()) {
                final String entity = generateIdEntity(prefix, type, p_mediaType);
                return Response.status(Response.Status.OK).entity(entity).build();
            } else {
                log.debug("RPT doesn't have enough permissions, access FORBIDDEN. Returns HTTP 403 (Forbidden).");
                return pair.getSecond();
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
    }

    private String generateIdEntity(String prefix, String type, String p_mediaType) throws IOException {
        final String id = generateId(prefix, type);
        if (p_mediaType.equals(MediaType.APPLICATION_JSON)) {
            return ServerUtil.asJson(new Id(id));
        } else if (p_mediaType.equals(MediaType.TEXT_PLAIN)) {
            return id;
        } else if (p_mediaType.equals(MediaType.TEXT_HTML)) {
            return "<html><title>" + IdType.fromString(type).getHtmlText() + "</title><body><h1>" + type + ": " + id + "</h1></body></html> ";
        } else if (p_mediaType.equals(MediaType.TEXT_XML)) {
            return "<?xml version=\"1.0\"?><inum type='" + IdType.fromString(type).getValue() + "'>" + id + "</inum>";
        }
        return "";
    }

    private String generateId(String prefix, String type) {
        final String id = idGenService.generateId(type, prefix);
        log.trace("Generated id: {0}, prefix: {1}, type: {2}", id, prefix, type);
        return id;
    }
}

