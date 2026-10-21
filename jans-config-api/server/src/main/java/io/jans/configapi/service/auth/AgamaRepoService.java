package io.jans.configapi.service.auth;

import com.fasterxml.jackson.databind.JsonNode;

import io.jans.as.model.util.Util;
import io.jans.configapi.service.status.StatusCheckerTimer;
import io.jans.util.exception.InvalidAttributeException;

import static io.jans.as.model.util.Util.escapeLog;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.util.Set;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;

@ApplicationScoped
public class AgamaRepoService {

    private static final String AGAMA_LAB_DOWNLOAD_PREFIX = "https://github.com/GluuFederation/";
    private static final URI AGAMA_LAB_BASE = URI.create(AGAMA_LAB_DOWNLOAD_PREFIX);
    private static final Set<String> RELEASE_ASSET_HOSTS = Set.of("release-assets.githubusercontent.com",
            "objects.githubusercontent.com");
    private static final int MAX_REDIRECTS = 5;
    private static final int TIMEOUT_MILLIS = 30000;

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
        if (!isAgamaLabUrl(uri)) {
            throw new InvalidAttributeException("Agama Project url must start with " + AGAMA_LAB_DOWNLOAD_PREFIX);
        }
        return Base64.encodeBase64(download(uri), true);
    }

    private boolean isAgamaLabUrl(URI uri) {
        String path = uri.getPath();
        return isPlainHttps(uri) && AGAMA_LAB_BASE.getHost().equals(uri.getHost()) && path != null
                && path.startsWith(AGAMA_LAB_BASE.getPath()) && !path.contains("..");
    }

    private boolean isReleaseAssetUrl(URI uri) {
        return isPlainHttps(uri) && RELEASE_ASSET_HOSTS.contains(uri.getHost());
    }

    private boolean isPlainHttps(URI uri) {
        return AGAMA_LAB_BASE.getScheme().equals(uri.getScheme()) && uri.getPort() == -1
                && uri.getRawUserInfo() == null;
    }

    // Manual redirects: a transferred repo must not lead outside the org
    private byte[] download(URI uri) throws IOException {
        URI current = uri;
        for (int hop = 0; hop <= MAX_REDIRECTS; hop++) {
            HttpURLConnection conn = (HttpURLConnection) current.toURL().openConnection();
            conn.setInstanceFollowRedirects(false);
            conn.setConnectTimeout(TIMEOUT_MILLIS);
            conn.setReadTimeout(TIMEOUT_MILLIS);
            try {
                int status = conn.getResponseCode();
                if (status >= 300 && status < 400) {
                    current = resolveRedirect(current, conn.getHeaderField("Location"));
                    continue;
                }
                if (status != HttpURLConnection.HTTP_OK) {
                    throw new IOException("Agama Project download failed with status " + status);
                }
                try (InputStream in = conn.getInputStream()) {
                    return IOUtils.toByteArray(in);
                }
            } finally {
                conn.disconnect();
            }
        }
        throw new IOException("Too many redirects downloading Agama Project");
    }

    private URI resolveRedirect(URI current, String location) throws IOException {
        if (location == null) {
            throw new IOException("Redirect without Location header");
        }
        URI next;
        try {
            next = current.resolve(new URI(location));
        } catch (URISyntaxException ex) {
            throw new IOException("Invalid redirect location", ex);
        }
        if (!isAgamaLabUrl(next) && !isReleaseAssetUrl(next)) {
            throw new InvalidAttributeException("Agama Project redirect target is not allowed");
        }
        return next;
    }
}