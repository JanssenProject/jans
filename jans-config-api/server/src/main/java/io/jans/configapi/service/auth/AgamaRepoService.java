package io.jans.configapi.service.auth;

import com.fasterxml.jackson.databind.JsonNode;

import io.jans.as.model.util.Util;
import io.jans.configapi.service.status.StatusCheckerTimer;
import io.jans.util.exception.InvalidAttributeException;

import static io.jans.as.model.util.Util.escapeLog;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;

@ApplicationScoped
public class AgamaRepoService {

    private static final String AGAMA_LAB_DOWNLOAD_PREFIX = "https://github.com/GluuFederation/";
    private static final String AGAMA_LAB_HOST = "github.com";
    private static final String AGAMA_LAB_PATH_PREFIX = "/GluuFederation/";

    @Inject
    private Logger logger;

    @Inject
    private StatusCheckerTimer statusCheckerTimer;

    public JsonNode getAllAgamaRepositories() {
        return statusCheckerTimer.getAllAgamaRepositories();
    }

    public byte[] getAgamaProject(String downloadLink) throws IOException {
        if (logger.isInfoEnabled()) {
            logger.info("Fetch Agama Project File from :{}", escapeLog(downloadLink));
        }
        if (StringUtils.isBlank(downloadLink)) {
            throw new InvalidAttributeException("Agama Project url is null!!!");
        }
        String url = URLDecoder.decode(downloadLink, Util.UTF8_STRING_ENCODING);
        if (logger.isInfoEnabled()) {
            logger.info("Decoded Agama Project url :{}", escapeLog(url));
        }
        if (!url.startsWith(AGAMA_LAB_DOWNLOAD_PREFIX)) {
            throw new InvalidAttributeException("Agama Project url must start with " + AGAMA_LAB_DOWNLOAD_PREFIX);
        }
        URI uri;
        try {
            uri = new URI(url);
        } catch (URISyntaxException ex) {
            throw new InvalidAttributeException("Invalid Agama Project url");
        }
        String path = uri.getPath();
        if (!"https".equals(uri.getScheme()) || !AGAMA_LAB_HOST.equals(uri.getHost()) || uri.getPort() != -1
                || uri.getRawUserInfo() != null || path == null || !path.startsWith(AGAMA_LAB_PATH_PREFIX) || path.contains("..")) {
            throw new InvalidAttributeException("Agama Project url must start with " + AGAMA_LAB_DOWNLOAD_PREFIX);
        }
        try (InputStream in = uri.toURL().openStream()) {
            return Base64.encodeBase64(IOUtils.toByteArray(in), true);
        }
    }

}