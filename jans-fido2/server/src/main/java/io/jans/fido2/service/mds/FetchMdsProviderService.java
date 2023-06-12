package io.jans.fido2.service.mds;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.jans.fido2.exception.mds.MdsClientException;
import io.jans.fido2.model.conf.AppConfiguration;
import io.jans.fido2.model.mds.MdsGetEndpointResponse;
import io.jans.fido2.service.DataMapperService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.apache.commons.lang3.StringUtils;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.slf4j.Logger;

import java.util.Collections;
import java.util.Map;

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
    public MdsGetEndpointResponse fetchMdsV3Endpoints(String endpoint) throws MdsClientException {
        Client client = clientBuilder.build();
        WebTarget target = client.target(endpoint + "/getEndpoints");
        Map<String, String> body = Collections.singletonMap("endpoint", appConfiguration.getBaseEndpoint());
        try {
            log.debug("Fetch mds getEndpoints request, body: {}", dataMapperService.writeValueAsString(body));
            Response response = target.request().post(Entity.entity(body, MediaType.APPLICATION_JSON_TYPE));
            String responseBody = response.readEntity(String.class);
            log.debug("Fetch mds getEndpoints response, body: {}", responseBody);
            MdsGetEndpointResponse responseEntity = dataMapperService.readValueString(responseBody, MdsGetEndpointResponse.class);
            if (!responseEntity.getStatus().equalsIgnoreCase("ok")) {
                throw new MdsClientException(String.format("Error getting endpoints from mds test, status: %s, errorMessage: '%s'", responseEntity.getStatus(), responseEntity.getErrorMessage()));
            }
            return responseEntity;
        } catch (JsonProcessingException e) {
            log.error("Error when processing json: {}", e.getMessage(), e);
            throw new MdsClientException("Error when processing json: " + e.getMessage());
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
