/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.configapi.rest.resource.auth;

import io.jans.configapi.service.auth.ConfigurationService;
import io.jans.as.client.service.OrgConfigurationService;
import io.jans.as.persistence.model.GluuOrganization;
import io.jans.configapi.service.auth.OrganizationService;
import io.jans.configapi.core.rest.ProtectedApi;
import io.jans.configapi.rest.model.AuthenticationMethod;
import io.jans.configapi.service.auth.ConfigurationService;
import io.jans.configapi.util.ApiAccessConstants;
import io.jans.configapi.util.ApiConstants;
import io.jans.configapi.util.AuthUtil;
import io.jans.configapi.core.util.Jackson;
import io.jans.model.GluuAttribute;
import io.jans.util.io.FileDownloader;
import io.jans.util.io.DownloadWrapper;
import io.jans.util.io.FileUploadWrapper;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import io.jans.util.StringHelper;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.io.IOUtils;

import org.slf4j.Logger;
import org.apache.commons.lang.StringUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.fge.jsonpatch.JsonPatchException;

@Path(ApiConstants.ORG)
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class OrganizationResource extends BaseResource {

    private static final String ORG_URL = "/jans-auth/restv1/internal/org";
    
    @Inject
    Logger log;
  
    @Inject
    AuthUtil authUtil;
  
    @Inject
    OrganizationService organizationService;

    @Context
    HttpServletRequest request;
    
    @Context
    HttpServletResponse response;

    
    @GET
    // @ProtectedApi(scopes = { ApiAccessConstants.ACRS_READ_ACCESS })
    public Response getGluuOrganization(@HeaderParam("Authorization") String authorization ) {
        final OrgConfigurationService orgConfigurationService = organizationService.getOrgConfigurationService(getOrganizationServiceUrl());
        log.error("\n\n OrganizationResource::getGluuOrganization() - orgConfigurationService:{}, request:{}, response:{} ", orgConfigurationService, request, response);
        
        Response response = orgConfigurationService.getOrg(authorization);
        GluuOrganization gluuOrganization = null;
        if (response.getStatus() == 200) {
            gluuOrganization = response.readEntity(GluuOrganization.class);
        }
        log.error("\n\n OrganizationResource::getGluuOrganization() - gluuOrganization:{} ", gluuOrganization);
        
        return Response.ok(gluuOrganization).build();
    }

    @PATCH
    @Consumes(MediaType.APPLICATION_JSON_PATCH_JSON)
    // @ProtectedApi(scopes = { ApiAccessConstants.ATTRIBUTES_WRITE_ACCESS })
    @Path(ApiConstants.INUM_PATH)
    public Response patchGluuOrganization(@NotNull String pathString) throws JsonPatchException, IOException {
        GluuOrganization gluuOrganization = organizationService.getOrganization();
        gluuOrganization = Jackson.applyPatch(pathString, gluuOrganization);
        organizationService.updateOrganization(gluuOrganization);
        return Response.ok(gluuOrganization).build();
    }
    
    private String getOrganizationServiceUrl() {
        return this.authUtil.getServiceUrl(ORG_URL);        
    }

/*
    private void saveLogo(FileUploadWrapper uploadedFile) {
        // Update LogoFile
        // Save file on server
        // Update GluuOrganization
        // Return GluuOrganization
        
        String fileName = saveFile(uploadedFile, BASE_AUTH_LOGO_PATH);
        final GluuOrganization gluuOrganization = organizationService.getOrganization();
        log.error("\n\n OrganizationResource::saveLogo() - gluuOrganization:{} ", gluuOrganization);

        if (StringUtils.isEmpty(fileName)) {
            throw new InternalServerErrorException(getInternalServerException("Error loading logo"));
        }
        gluuOrganization.setJsLogoPath(BASE_AUTH_LOGO_PATH + fileName);
        organizationService.updateOrganization(gluuOrganization);

    }

    private void saveFavIcon(FileUploadWrapper uploadedFile) {
        String fileName = saveFile(uploadedFile, BASE_AUTH_FAVICON_PATH);
        final GluuOrganization gluuOrganization = organizationService.getOrganization();
        log.error("\n\n OrganizationResource::saveFavIcon() - gluuOrganization:{} ", gluuOrganization);

        if (StringUtils.isEmpty(fileName)) {
            throw new InternalServerErrorException(getInternalServerException("Error loading favicon"));
        }
        gluuOrganization.setJsFaviconPath(BASE_AUTH_LOGO_PATH + fileName);
        organizationService.updateOrganization(gluuOrganization);

    }
    */
    
    private String saveFile(FileUploadWrapper fileUploadWrapper, String basePath) {
        log.error("\n\n OrganizationResource::saveFile() - fileUploadWrapper:{} , basePath:{} ", fileUploadWrapper,
                basePath);
        String fileName = fileUploadWrapper.getFileName();
        try {
            File file = new File(basePath, fileName);
            if (!file.exists()) {
                File dir = new File(basePath);
                if (!dir.exists()) {
                    dir.mkdir();
                }
                file.createNewFile();
                file = new File(basePath, fileName);
            }
            Files.copy(fileUploadWrapper.getStream(), file.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ex) {
            log.error("Error loading custom idp favicon", ex);
            return null;
        } finally {
            fileUploadWrapper = null;
        }
        return fileName;
    }

    public FileUploadWrapper getFileUploadWrapper(String file) {
        return null;
    }
    
    /*
    private boolean readCustomFavicon(HttpServletResponse response, GluuOrganization organization) {
        if (organization.getJsFaviconPath() == null || StringUtils.isEmpty(organization.getJsFaviconPath())) {
            return false;
        }

        File directory = new File(BASE_OXAUTH_FAVICON_PATH);
        if (!directory.exists()) {
            directory.mkdir();
        }
        File faviconPath = new File(organization.getJsFaviconPath());
        if (!faviconPath.exists()) {
            return false;
        }
        try (InputStream in = new FileInputStream(faviconPath); OutputStream out = response.getOutputStream()) {
            IOUtils.copy(in, out);
            return true;
        } catch (IOException e) {
            log.debug("Error loading custom favicon: " + e.getMessage());
            return false;
        }
    }


    private boolean readDefaultFavicon(HttpServletResponse response) {
        String defaultFaviconFileName = "/WEB-INF/static/favicon.ico";
        
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
                try (InputStream in = loader.getResourceAsStream(defaultFaviconFileName);
             OutputStream out = response.getOutputStream()) {
            IOUtils.copy(in, out);
            return true;
        } catch (IOException e) {
            log.debug("Error loading default favicon: " + e.getMessage());
            return false;
        }
    }*/

}