/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.configapi.rest.resource.auth;

import com.github.fge.jsonpatch.JsonPatchException;
import io.jans.configapi.filters.ProtectedApi;
import io.jans.configapi.service.auth.LdapConfigurationService;
import io.jans.configapi.util.ApiAccessConstants;
import io.jans.configapi.util.ApiConstants;
import io.jans.configapi.util.ConnectionStatus;
import io.jans.configapi.util.Jackson;
import io.jans.model.ldap.GluuLdapConfiguration;
import org.slf4j.Logger;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.List;
import java.util.NoSuchElementException;

@Path(ApiConstants.CONFIG + ApiConstants.DATABASE + ApiConstants.LDAP)
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class LdapConfigurationResource extends BaseResource {

    @Inject
    Logger log;

    @Inject
    LdapConfigurationService ldapConfigurationService;

    @Inject
    ConnectionStatus connectionStatus;

    @GET
    @ProtectedApi(scopes = { ApiAccessConstants.DATABASE_LDAP_READ_ACCESS })
    public Response getLdapConfiguration() {
        List<GluuLdapConfiguration> ldapConfigurationList = this.ldapConfigurationService.findLdapConfigurations();
        return Response.ok(ldapConfigurationList).build();
    }

    @GET
    @Path(ApiConstants.NAME_PARAM_PATH)
    @ProtectedApi(scopes = { ApiAccessConstants.DATABASE_LDAP_READ_ACCESS })
    public Response getLdapConfigurationByName(@PathParam(ApiConstants.NAME) String name) {
        GluuLdapConfiguration ldapConfiguration = findLdapConfigurationByName(name);
        return Response.ok(ldapConfiguration).build();
    }

    @POST
    @ProtectedApi(scopes = { ApiAccessConstants.DATABASE_LDAP_WRITE_ACCESS })
    public Response addLdapConfiguration(@Valid @NotNull GluuLdapConfiguration ldapConfiguration) {
        log.debug("LDAP configuration to be added - ldapConfiguration = "+ldapConfiguration);
        // Ensure that an LDAP server with same name does not exists.
        try {
            ldapConfiguration = findLdapConfigurationByName(ldapConfiguration.getConfigId());
            log.error("Ldap Configuration with same name '" + ldapConfiguration.getConfigId() + "' already exists!");
            throw new NotAcceptableException(getNotAcceptableException(
                    "Ldap Configuration with same name - '" + ldapConfiguration.getConfigId() + "' already exists!"));
        } catch (NotFoundException ne) {
            this.ldapConfigurationService.save(ldapConfiguration);
            ldapConfiguration = findLdapConfigurationByName(ldapConfiguration.getConfigId());
            return Response.status(Response.Status.CREATED).entity(ldapConfiguration).build();
        }
    }

    @PUT
    @ProtectedApi(scopes = { ApiAccessConstants.DATABASE_LDAP_WRITE_ACCESS })
    public Response updateLdapConfiguration(@Valid @NotNull GluuLdapConfiguration ldapConfiguration) {
        log.debug("LDAP configuration to be updated - ldapConfiguration = "+ldapConfiguration);
        findLdapConfigurationByName(ldapConfiguration.getConfigId());
        this.ldapConfigurationService.update(ldapConfiguration);
        return Response.ok(ldapConfiguration).build();
    }

    @DELETE
    @Path(ApiConstants.NAME_PARAM_PATH)
    @ProtectedApi(scopes = { ApiAccessConstants.DATABASE_LDAP_DELETE_ACCESS })
    public Response deleteLdapConfigurationByName(@PathParam(ApiConstants.NAME) String name) {
        log.debug("LDAP configuration to be deleted - name = "+name);
        findLdapConfigurationByName(name);
        log.info("Delete Ldap Configuration by name " + name);
        this.ldapConfigurationService.remove(name);
        return Response.noContent().build();
    }

    @PATCH
    @Path(ApiConstants.NAME_PARAM_PATH)
    @Consumes(MediaType.APPLICATION_JSON_PATCH_JSON)
    @ProtectedApi(scopes = { ApiAccessConstants.DATABASE_LDAP_WRITE_ACCESS })
    public Response patchLdapConfigurationByName(@PathParam(ApiConstants.NAME) String name,
            @NotNull String requestString) throws JsonPatchException, IOException {
        log.debug("LDAP configuration to be patched - name = "+name+" , requestString = "+requestString);
        GluuLdapConfiguration ldapConfiguration = findLdapConfigurationByName(name);
        log.info("Patch Ldap Configuration by name " + name);
        ldapConfiguration = Jackson.applyPatch(requestString, ldapConfiguration);
        this.ldapConfigurationService.update(ldapConfiguration);
        return Response.ok(ldapConfiguration).build();
    }

    @POST
    @Path(ApiConstants.TEST)
    @ProtectedApi(scopes = { ApiAccessConstants.DATABASE_LDAP_READ_ACCESS })
    public Response testLdapConfigurationByName(@Valid @NotNull GluuLdapConfiguration ldapConfiguration) {
        log.debug("LDAP configuration to be tested - ldapConfiguration = "+ldapConfiguration);
        log.info("Test ldapConfiguration " + ldapConfiguration);
        boolean status = connectionStatus.isUp(ldapConfiguration);
        log.info("\n\n\n LdapConfigurationResource:::testLdapConfigurationByName() - status = " + status + "\n\n\n");
        return Response.ok(status).build();
    }

    private GluuLdapConfiguration findLdapConfigurationByName(String name) {
        try {
            return this.ldapConfigurationService.findByName(name);
        } catch (NoSuchElementException ex) {
            log.error("Could not find Ldap Configuration by name '" + name + "'", ex);
            throw new NotFoundException(getNotFoundError("Ldap Configuration - '" + name + "'"));
        }
    }
}
