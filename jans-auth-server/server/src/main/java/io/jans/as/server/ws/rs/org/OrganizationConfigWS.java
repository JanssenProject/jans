package io.jans.as.server.ws.rs.org;

import io.jans.as.common.service.OrganizationService;
import io.jans.util.io.FileUploadWrapper;
import io.jans.util.io.DownloadWrapper;
import io.jans.as.model.common.ComponentType;
import io.jans.as.model.common.Image;
import io.jans.as.model.configuration.AppConfiguration;
import io.jans.as.model.error.ErrorResponseFactory;
import io.jans.as.model.authorize.AuthorizeErrorResponseType;
import io.jans.as.persistence.model.GluuOrganization;
import io.jans.as.server.model.common.AbstractToken;
import io.jans.as.server.model.common.AuthorizationGrant;
import io.jans.as.server.service.stat.StatService;
import io.jans.as.server.service.token.TokenService;
import io.jans.as.server.util.ServerUtil;
import io.jans.orm.PersistenceEntryManager;
import io.jans.orm.search.filter.Filter;
import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.Counter;
import io.prometheus.client.exporter.common.TextFormat;
import net.agkn.hll.HLL;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletContext;
import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static io.jans.as.model.util.Util.escapeLog;

@ApplicationScoped
@Path("/internal/org")
public class OrganizationConfigWS {

    public static final String BASE_AUTH_FAVICON_PATH = "/opt/jans/jetty/jans-auth/custom/static/favicon/";
    public static final String DEFAULT_FAVICON_FILENAME = "/WEB-INF/static/favicon.ico";

    public static final String BASE_AUTH_LOGO_PATH = "/opt/jans/jetty/jans-auth/custom/static/logo/";
    public static final String DEFAULT_LOGO_FILENAME = "/WEB-INF/static/logo.png";

    @Inject
    private Logger log;

    @Inject
    private ErrorResponseFactory errorResponseFactory;

    @Inject
    private OrganizationService organizationService;

    @Context
    HttpServletRequest request;

    @Context
    HttpServletResponse response;

    @Context
    private ServletContext context;

    @GET
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    public Response getOrgData(@HeaderParam("Authorization") String authorization) {
        log.error("\n\n OrganizationConfigWS::getOrgData() - authorization:{}, request:{}", authorization, request);
        GluuOrganization gluuOrganization = organizationService.getOrganization();
        log.error("\n\n OrganizationConfigWS::getOrgData() - new - gluuOrganization:{}", gluuOrganization);
        return Response.ok(gluuOrganization).build();
    }

    @GET
    @Path("{imageType}")
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    public Response getImage(@HeaderParam("Authorization") String authorization,
            @PathParam("imageType") Image imageType) {
        log.error("\n\n OrganizationConfigWS::getImage() - authorization:{}, imageType:{}", authorization, imageType);

        if (imageType == null) {
            throw errorResponseFactory.createWebApplicationException(Response.Status.INTERNAL_SERVER_ERROR,
                    AuthorizeErrorResponseType.SERVER_ERROR, "Image type cannot blank.");
        }

        GluuOrganization gluuOrganization = organizationService.getOrganization();
        log.error("\n\n OrganizationConfigWS::getImage() - gluuOrganization:{}", gluuOrganization);
        
        boolean success = readDefaultFavicon(response);
        log.error("\n\n OrganizationConfigWS::getImage() - success:{}", success);

        DownloadWrapper downloadWrapper = readImage(imageType, gluuOrganization);
        log.error("\n\n OrganizationConfigWS::getImage() - downloadWrapper:{}", downloadWrapper);

        

        return Response.ok(downloadWrapper).build();
    }

    @PUT
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    public Response updateOrg(@HeaderParam("Authorization") String authorization, GluuOrganization gluuOrganization) {
        log.error("\n\n OrganizationConfigWS::updateOrg() - gluuOrganization:{}", gluuOrganization);
        organizationService.updateOrganization(gluuOrganization);
        gluuOrganization = organizationService.getOrganization();
        log.error("\n\n OrganizationConfigWS::updateOrg() - gluuOrganization:{}", gluuOrganization);
        return Response.ok(gluuOrganization).build();
    }

    @PUT
    @Path("{imageType}")
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    public Response updateImageResource(@HeaderParam("Authorization") String authorization,
            @PathParam("imageType") Image image, FileUploadWrapper fileUploadWrapper) {
        log.error("\n\n OrganizationConfigWS::updateImageResource() - authorization:{}, image:{}, fileUploadWrapper:{}",
                authorization, image, fileUploadWrapper);
        GluuOrganization gluuOrganization = saveImage(fileUploadWrapper, image);
        log.error("\n\n OrganizationConfigWS::updateImageResource() - gluuOrganization:{} ", gluuOrganization);

        return Response.ok(gluuOrganization).build();
    }

    private GluuOrganization saveImage(FileUploadWrapper uploadedFile, Image image) {
        // Save file on server
        // Update GluuOrganization
        // Return GluuOrganization

        String basePath = getBasePath(image);
        log.error("\n\n OrganizationConfigWS::saveImage() - basePath:{} ", basePath);
        if (StringUtils.isBlank(basePath)) {
            throw errorResponseFactory.createWebApplicationException(Response.Status.INTERNAL_SERVER_ERROR,
                    AuthorizeErrorResponseType.SERVER_ERROR, "Error while uploading image - base path blank.");
        }

        String fileName = saveFile(uploadedFile, basePath);
        log.error("\n\n OrganizationConfigWS::saveImage() - fileName:{} ", fileName);

        final GluuOrganization gluuOrganization = organizationService.getOrganization();
        log.error("\n\n OrganizationConfigWS::saveImage() - gluuOrganization:{} ", gluuOrganization);

        if (image == Image.FAVICON) {
            gluuOrganization.setJsFaviconPath(basePath + fileName);
        } else if (image == Image.LOGO) {
            gluuOrganization.setJsLogoPath(basePath + fileName);
        }

        organizationService.updateOrganization(gluuOrganization);
        return organizationService.getOrganization();

    }

    private String saveFile(FileUploadWrapper fileUploadWrapper, String basePath) {
        log.error("\n\n OrganizationConfigWS::saveFile() - fileUploadWrapper:{} , basePath:{} ", fileUploadWrapper,
                basePath);

        String fileName = fileUploadWrapper.getFileName();
        log.error("\n\n OrganizationConfigWS::saveFile() - fileName:{} ", fileName);
        if (StringUtils.isBlank(fileName)) {
            throw errorResponseFactory.createWebApplicationException(Response.Status.INTERNAL_SERVER_ERROR,
                    AuthorizeErrorResponseType.SERVER_ERROR, "Error while uploading image - file name blank.");
        }

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

    private String getBasePath(Image image) {
        switch (image) {
        case FAVICON:
            return BASE_AUTH_FAVICON_PATH;
        case LOGO:
            return BASE_AUTH_LOGO_PATH;
        }
        return null;

    }

    private String getCustomFileName(Image image, GluuOrganization gluuOrganization) {
        switch (image) {
        case FAVICON:
            return gluuOrganization.getJsFaviconPath();
        case LOGO:
            return gluuOrganization.getJsLogoPath();
        }
        return null;
    }

    private String getDefaultFileName(Image image) {
        switch (image) {
        case FAVICON:
            return DEFAULT_FAVICON_FILENAME;
        case LOGO:
            return DEFAULT_LOGO_FILENAME;
        }
        return null;
    }

    private DownloadWrapper readImage(Image imageType, GluuOrganization gluuOrganization) {
        log.error("\n\n OrganizationConfigWS::readImage() - imageType:{}, gluuOrganization:{}", imageType,
                gluuOrganization);

        DownloadWrapper downloadWrapper = readCustomImage(imageType, gluuOrganization);
        log.error("\n\n OrganizationConfigWS::readImage() - 1 - downloadWrapper:{} ", downloadWrapper);
        if (downloadWrapper == null || downloadWrapper.getStream() == null) {
            log.error(" Reading default image as custom image is null ");
            downloadWrapper = readDefaultImage(imageType);
            log.error("\n\n OrganizationConfigWS::readImage() - 2 - downloadWrapper:{} ", downloadWrapper);
        }

        return downloadWrapper;

    }

    private DownloadWrapper readCustomImage(Image imageType, GluuOrganization gluuOrganization) {
        log.error("\n\n OrganizationConfigWS::readCustomImage() - imageType:{}, gluuOrganization:{}", imageType,
                gluuOrganization);
        String fileName = getCustomFileName(imageType, gluuOrganization);
        if (StringUtils.isBlank(fileName)) {
            return null;
        }

        log.error("\n\n OrganizationConfigWS::readCustomImage() - 1");
        File directory = new File(getBasePath(imageType));
        if (!directory.exists()) {
            directory.mkdir();
        }

        log.error("\n\n OrganizationConfigWS::readCustomImage() - 2");
        File file = new File(fileName);
        if (!file.exists()) {
            return null;
        }

        try (InputStream in = getClass().getClassLoader().getResourceAsStream(fileName)) {
            log.error("\n\n OrganizationConfigWS::readCustomImage() - 3 - InputStream:{}", in);
            // IOUtils.copy(in, out);
            return (new DownloadWrapper(in, fileName, null, new Date(file.lastModified())));
        } catch (IOException e) {
            log.error("Error loading custom image - imageType:{}, fileName:{}, exception:{} ", imageType, fileName,
                    e.getMessage());
            return null;
        }

        /*
         * try (InputStream in = new FileInputStream(fileName)) {
         * log.error("\n\n OrganizationConfigWS::readCustomImage() - 3 - InputStream:{}"
         * , in); return (new DownloadWrapper(in, fileName, null, new
         * Date(file.lastModified()))); } catch (IOException e) { log.
         * error("Error loading custom image - imageType:{}, fileName:{}, exception:{} "
         * , imageType, fileName, e.getMessage()); return null; }
         */
    }

    private boolean readDefaultFavicon(HttpServletResponse response) {
        String defaultFaviconFileName = "/WEB-INF/static/favicon.ico";
        try (InputStream in = context.getResourceAsStream(defaultFaviconFileName);
                OutputStream out = response.getOutputStream()) {
            log.error("\n\n OrganizationConfigWS::readDefaultFavicon() - in:{}", in);
            IOUtils.copy(in, out);
            log.error("\n\n OrganizationConfigWS::readDefaultFavicon() - out:{}", out);
            return true;
        } catch (IOException e) {
            log.debug("Error loading default favicon: " + e.getMessage());
            return false;
        }
    }

    private DownloadWrapper readDefaultImage(Image imageType) {
        log.error("\n\n OrganizationConfigWS::readDefaultImage() - 1 - imageType:{}", imageType);
        String fileName = getDefaultFileName(imageType);

        File file = new File(fileName);
        log.error("\n\n OrganizationConfigWS::readDefaultImage() - 2 - file.exists(1):{}", file.exists());

        log.error(
                "\n\n OrganizationConfigWS::readDefaultImage() - 3 - context.getContextPath():{} , context.getContextPath()+fileName:{} ",
                context.getContextPath(), context.getContextPath() + fileName);
        file = new File(context.getContextPath() + fileName);
        log.error("\n\n OrganizationConfigWS::readDefaultImage() - file.exists(2):{}", file.exists());

        if (file.exists()) {
            try {
                log.error(
                        "\n\n OrganizationConfigWS::readDefaultImage() - 4 -  File exists - file.getPath():{}, file.canRead():{}, file.getCanonicalPath(),  file.toURI():{}",
                        file.getPath(), file.canRead(), file.getCanonicalPath(), file.toURI());
            } catch (IOException e) {
                log.error(
                        "Error loading reading file properties for DefaultImage - imageType:{}, fileName:{}, exception:{} ",
                        imageType, fileName, e.getMessage());
                return null;
            }
        }

        log.error(
                "\n\n OrganizationConfigWS::readDefaultImage() - 5 - file.getAbsolutePath():{}, file.getAbsoluteFile():{}",
                file.getAbsolutePath(), file.getAbsoluteFile());

        InputStream inputStream = getClass().getClassLoader().getResourceAsStream(fileName);
        log.error("\n\n OrganizationConfigWS::readDefaultImage() - 6 - getClass().getClassLoader() - inputStream:{}",
                inputStream);

        inputStream = getClass().getClassLoader().getResourceAsStream(file.getAbsolutePath());
        log.error("\n\n OrganizationConfigWS::readDefaultImage() - 7 - file.getAbsolutePath():{}, inputStream:{}",
                file.getAbsolutePath(), inputStream);

        inputStream = context.getResourceAsStream("/WEB-INF/test/foo.txt");
        log.error("\n\n OrganizationConfigWS::readDefaultImage() - 8 -  context.getResourceAsStream  inputStream:{}",
                inputStream);

        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        inputStream = classLoader.getResourceAsStream(fileName);
        log.error("\n\n OrganizationConfigWS::readDefaultImage() - 9 - context.getResourceAsStream  inputStream:{}",
                inputStream);

        InputStreamReader isr = new InputStreamReader(
                OrganizationConfigWS.class.getClassLoader().getResourceAsStream(fileName));
        log.error("\n\n OrganizationConfigWS::readDefaultImage() - 10 - InputStreamReader  isr:{}", isr);
        
        
        String path = Thread.currentThread().getContextClassLoader().getResource(fileName).getPath();
        File f = new File(path);
        log.error("\n\n OrganizationConfigWS::readDefaultImage() - 11 -  fileName:{}, path:{}, f.getAbsolutePath():{}", fileName, path, f.getAbsolutePath());
        

        try (InputStream in = new FileInputStream(fileName)) {
            log.error("\n\n OrganizationConfigWS::readDefaultImage() - 12 - InputStream:{}", in);
            return (new DownloadWrapper(in, fileName, null, new Date(file.lastModified())));
        } catch (IOException e) {
            log.error("Error loading default image - imageType:{}, fileName:{}, exception:{} ", imageType, fileName,
                    e.getMessage());
            return null;
        }
    }

}
