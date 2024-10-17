package io.jans.fido2.service.mds;

import io.jans.fido2.exception.mds.MdsClientException;
import io.jans.fido2.model.conf.AppConfiguration;
import io.jans.fido2.service.DataMapperService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.Response;
import org.apache.commons.lang3.StringUtils;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.slf4j.Logger;

@ApplicationScoped
public class FetchMdsProviderService {

    @Inject
    private Logger log;

    @Inject
    private AppConfiguration appConfiguration;

    @Inject
    private DataMapperService dataMapperService;

    private final ClientBuilder clientBuilder = ResteasyClientBuilder.newBuilder();

    /**
     * Fetch mds getEndpoints
     *
     * @return MetadataTestResponse class
     * @throws MdsClientException When an attempt is made to process the json or the status returns other than 200
     */
    public String fetchMdsV3Endpoints(String endpoint) throws MdsClientException {
        Client client = clientBuilder.build();
        WebTarget target = client.target(endpoint);
        try {
            Response response = target.request().get();
            if (response.getStatus() != 200) {
                throw new MdsClientException(String.format("Error getting endpoints from mds test, status: %s, errorMessage: '%s'", response.getStatus(), response.getStatusInfo().getReasonPhrase()));
            }
            String responseBody = response.readEntity(String.class);
            log.debug("Fetch mds getEndpoints response, body: {}", responseBody);
            return responseBody;
        } finally {
            client.close();
        }
    }

    /**
     * Fetch metadata Blob (TOC)
     *
     * @param mdsUrl url Blob (TOC)
     * @return String of Json Web Token (JWT) or null in case of error
     */
    public String fetchMetadataBlob(String mdsUrl) {
        Client client = clientBuilder.build();
        WebTarget target = client.target(mdsUrl);
        try {
            log.debug("Fetch mds Blob (TOC) request, mdsUrl: {}", mdsUrl);
            Response response = target.request().get();
            if (response.getStatus() != 200) {
                log.error("Error when get blob: status: {}", response.getStatus());
                return null;
            }
            String responseBody = response.readEntity(String.class);
            log.debug("Fetch mds Blob (TOC) response, body: {}", (responseBody.length() > 100 ? StringUtils.abbreviateMiddle(responseBody, "...", 100) : responseBody));
            return responseBody;
        } finally {
            client.close();
        }
    }
}
