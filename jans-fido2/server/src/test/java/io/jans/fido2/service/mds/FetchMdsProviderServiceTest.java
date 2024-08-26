package io.jans.fido2.service.mds;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.nimbusds.jose.JWSObject;
import io.jans.fido2.exception.mds.MdsClientException;
import io.jans.fido2.model.conf.AppConfiguration;
import io.jans.fido2.model.mds.MdsGetEndpointResponse;
import io.jans.fido2.service.DataMapperService;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.apache.commons.lang3.StringUtils;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;

import java.text.ParseException;
import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FetchMdsProviderServiceTest {

    @InjectMocks
    private FetchMdsProviderService fetchMdsProviderService;

    @Mock
    private Logger log;

    @Mock
    private AppConfiguration appConfiguration;

    @Mock
    private DataMapperService dataMapperService;


    @Test
    void fetchMdsV3Endpoints_withValidEndpoint_valid() throws MdsClientException, JsonProcessingException {
        String endpoint = "https://mds3.fido.tools/";
        String response = fetchMdsProviderService.fetchMdsV3Endpoints(endpoint);
        assertNotNull(response);
        verify(log, times(1)).debug(contains("Fetch mds getEndpoints response, body:"), anyString());
    }

    @Test
    void fetchMetadataBlob_withValidMdsUrl_valid() throws ParseException {
        MdsGetEndpointResponse responseGetEndpoint = ResteasyClientBuilder.newBuilder().build()
                .target("https://mds3.fido.tools/getEndpoints").request()
                .post(Entity.entity(Collections.singletonMap("endpoint", "https://jans.fido.org"), MediaType.APPLICATION_JSON_TYPE))
                .readEntity(MdsGetEndpointResponse.class);
        assertNotNull(responseGetEndpoint);
        assertFalse(responseGetEndpoint.getResult().isEmpty());

        String mdsUrl = responseGetEndpoint.getResult().get(0);

        String response = fetchMdsProviderService.fetchMetadataBlob(mdsUrl);
        assertNotNull(response);
        JWSObject jwt = JWSObject.parse(response);
        assertNotNull(jwt);
        assertNotNull(jwt.getHeader());
        assertNotNull(jwt.getPayload());

        verify(log).debug("Fetch mds Blob (TOC) request, mdsUrl: {}", mdsUrl);
        verify(log).debug("Fetch mds Blob (TOC) response, body: {}", StringUtils.abbreviateMiddle(response, "...", 100));
        verifyNoMoreInteractions(log);
    }

    @Test
    void fetchMetadataBlob_withInvalidMdsUrl_null() {
        String mdsUrl = "https://mds3.fido.tools/execute/wrong_mds";

        String response = fetchMdsProviderService.fetchMetadataBlob(mdsUrl);
        assertNull(response);

        verify(log).debug("Fetch mds Blob (TOC) request, mdsUrl: {}", mdsUrl);
        verify(log).error(contains("Error when get blob: status"), anyInt());
        verifyNoMoreInteractions(log);
    }
}
