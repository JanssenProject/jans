/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.configapi.rest.resource.auth;

import io.jans.configapi.service.auth.OrganizationService;
import io.jans.as.persistence.model.GluuOrganization;
import io.jans.configapi.filters.ProtectedApi;
import io.jans.configapi.rest.model.AuthenticationMethod;
import io.jans.configapi.service.auth.ConfigurationService;
import io.jans.configapi.util.ApiAccessConstants;
import io.jans.configapi.util.ApiConstants;
import io.jans.configapi.util.Jackson;
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
import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.io.IOUtils;

import org.slf4j.Logger;
import org.apache.commons.lang.StringUtils;
import com.github.fge.jsonpatch.JsonPatchException;

@Path(ApiConstants.ORG)
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class OrganizationResource extends BaseResource {

    @Inject
    Logger log;

    @Inject
    OrganizationService organizationService;

    @Inject
    HttpServletRequest request;

    @Inject
    HttpServletResponse response;

    public static final String BASE_AUTH_FAVICON_PATH = "/opt/jans/jetty/jans-auth/custom/static/favicon/";
    public static final String BASE_AUTH_LOGO_PATH = "/opt/jans/jetty/jans-auth/custom/static/logo/";

    @GET
    // @ProtectedApi(scopes = { ApiAccessConstants.ACRS_READ_ACCESS })
    public Response getGluuOrganization() {
        final GluuOrganization gluuOrganization = organizationService.getOrganization();
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

    private void saveLogo(FileUploadWrapper uploadedFile) {
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

    private boolean readDefaultFavicon() {
        log.error("\n\n OrganizationResource::readDefaultFavicon() - response:{}", response);
        // String defaultFaviconFileName = "/WEB-INF/static/favicon.ico";
        String defaultFaviconFileName = "/WEB-INF/static/favicon.ico";

        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        try (InputStream input = loader.getResourceAsStream(defaultFaviconFileName);
                OutputStream out = response.getOutputStream()) {
            IOUtils.copy(input, out);
            return true;
        } catch (IOException e) {
            log.debug("Error loading default favicon: " + e.getMessage());
            return false;
        }
    }

}