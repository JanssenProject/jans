/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.configapi.rest.resource.auth;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.fge.jsonpatch.JsonPatchException;

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
import io.jans.util.StringHelper;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Map;

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
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;

import javax.ws.rs.core.MultivaluedMap;
//import org.jboss.resteasy.plugins.providers.multipart.InputPart;
//import org.jboss.resteasy.plugins.providers.multipart.MultipartFormDataInput;

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
    // @ProtectedApi(scopes = { ApiAccessConstants.ORG_READ_ACCESS })
    public Response getGluuOrganization() {
        return Response.ok(organizationService.getOrganization()).build();
    }

    @PUT
    // @ProtectedApi(scopes = { ApiAccessConstants.ORG_WRITE_ACCESS })
    public Response updateOrg(GluuOrganization organization) {
        log.error("\n\n OrganizationResource::updateOrg() -  organization:{} ", organization);
        organizationService.updateOrganization(organization);
        return Response.ok(organizationService.getOrganization()).build();
    }

    @PATCH
    @Consumes(MediaType.APPLICATION_JSON_PATCH_JSON)
    // @ProtectedApi(scopes = { ApiAccessConstants.ORG_WRITE_ACCESS })
    public Response patchGluuOrganization(@NotNull String pathString) throws JsonPatchException, IOException {
        GluuOrganization gluuOrganization = organizationService.getOrganization();
        gluuOrganization = Jackson.applyPatch(pathString, gluuOrganization);
        organizationService.updateOrganization(gluuOrganization);
        return Response.ok(gluuOrganization).build();
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

    /*
     * @POST
     * 
     * @Path("/upload")
     * 
     * @Consumes("multipart/form-data") public Response
     * uploadFile(MultipartFormDataInput input) {
     * log.error("\n\n OrganizationResource::uploadFile() - input:{} ", input);
     * String fileName = "";
     * 
     * Map<String, List<InputPart>> formParts = input.getFormDataMap();
     * log.error("\n\n OrganizationResource::uploadFile() - formParts:{} ",
     * formParts);
     * 
     * List<InputPart> inPart = formParts.get("file");
     * log.error("\n\n OrganizationResource::uploadFile() - inPart:{} ", inPart);
     * for (InputPart inputPart : inPart) {
     * 
     * try {
     * 
     * // Retrieve headers, read the Content-Disposition header to obtain the //
     * original name of the file MultivaluedMap<String, String> headers =
     * (MultivaluedMap) inputPart.getHeaders();
     * 
     * fileName = parseFileName(headers);
     * log.error("\n\n OrganizationResource::uploadFile() - fileName:{} ",
     * fileName); // Handle the body of that part with an InputStream InputStream
     * InputStream istream = inputPart.getBody(InputStream.class, null);
     * 
     * log.error("\n\n OrganizationResource::uploadFile() - istream:{} ", istream);
     * 
     * } catch (IOException e) { e.printStackTrace(); }
     * 
     * }
     * 
     * String output = "File saved to server location : " + fileName;
     * 
     * return Response.status(200).entity(output).build(); }
     * 
     */

    // Parse Content-Disposition header to get the original file name
    private String parseFileName(MultivaluedMap<String, String> headers) {

        String[] contentDispositionHeader = headers.getFirst("Content-Disposition").split(";");

        for (String name : contentDispositionHeader) {

            if ((name.trim().startsWith("filename"))) {

                String[] tmp = name.split("=");

                String fileName = tmp[1].trim().replaceAll("\"", "");

                return fileName;
            }
        }
        return "randomName";
    }

}