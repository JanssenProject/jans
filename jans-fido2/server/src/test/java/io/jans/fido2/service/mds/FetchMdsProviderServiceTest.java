package io.jans.fido2.service.mds;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.nimbusds.jose.JWSObject;
import io.jans.fido2.exception.mds.MdsClientException;
import io.jans.fido2.model.conf.AppConfiguration;
import io.jans.fido2.model.mds.MdsGetEndpointResponse;
import io.jans.fido2.service.DataMapperService;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;

import java.text.ParseException;
import java.util.Arrays;

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
        String baseEndpoint = "https://base-fido-serverfido.test.url";
        when(appConfiguration.getBaseEndpoint()).thenReturn(baseEndpoint);
        when(dataMapperService.writeValueAsString(any())).thenReturn("{\"endpoint\":\"" + baseEndpoint + "\"}");
        MdsGetEndpointResponse mdsGetEndpointResponse = new MdsGetEndpointResponse();
        mdsGetEndpointResponse.setStatus("ok");
        mdsGetEndpointResponse.setResult(Arrays.asList(
                "https://mds3.fido.tools/execute/5c5f5c14e804288bead66d5fd5c54fbb7c773b8d6786279e708e842554e70a29",
                "https://mds3.fido.tools/execute/0cb1551242b4a32d5e7e7cbac3db8d175f03c5ca71f2374417eb9ebee922523a",
                "https://mds3.fido.tools/execute/a4ab3537c91c901902f2b7f9ea6540723a7c068382bb79acb3ed310a51831e36",
                "https://mds3.fido.tools/execute/3798ae5a0d2cd99a928604a462d475cd555014c244a44ba0cb1b61fc644b7b2b",
                "https://mds3.fido.tools/execute/02f495ce54cc6ffcbd4b50f0c539bf8b76c8feee3e17c28445c52cfc3baf0852"
        ));
        when(dataMapperService.readValueString(any(), eq(MdsGetEndpointResponse.class))).thenReturn(mdsGetEndpointResponse);

        MdsGetEndpointResponse response = fetchMdsProviderService.fetchMdsV3Endpoints(endpoint);
        assertNotNull(response);
        assertNotNull(response.getStatus());
        assertNotNull(response.getResult());
        assertEquals(response.getStatus(), "ok");
        assertFalse(response.getResult().isEmpty());
        response.getResult().forEach(Assertions::assertNotNull);

        verify(log, times(2)).debug(contains("Fetch mds getEndpoints"), anyString());
    }

    @Test
    void fetchMdsV3Endpoints_withEmptyEndpoint_mdsClientException() throws JsonProcessingException {
        String endpoint = "https://mds3.fido.tools/";
        String baseEndpoint = "https://base-fido-serverfido.test.url";
        when(appConfiguration.getBaseEndpoint()).thenReturn(baseEndpoint);
        when(dataMapperService.writeValueAsString(any())).thenReturn("{\"endpoint\":\"" + baseEndpoint + "\"}");
        MdsGetEndpointResponse mdsGetEndpointResponse = new MdsGetEndpointResponse();
        mdsGetEndpointResponse.setStatus("failed");
        mdsGetEndpointResponse.setErrorMessage("Request missing endpoint field!");
        when(dataMapperService.readValueString(any(), eq(MdsGetEndpointResponse.class))).thenReturn(mdsGetEndpointResponse);

        MdsClientException response = assertThrows(MdsClientException.class, () -> fetchMdsProviderService.fetchMdsV3Endpoints(endpoint));
        assertNotNull(response);
        assertNotNull(response.getMessage());
        assertTrue(response.getMessage().contains("Error getting endpoints"));

        verify(log, times(2)).debug(contains("Fetch mds getEndpoints"), anyString());
    }

    @Test
    void fetchMetadataBlob_withValidMdsUrl_valid() throws ParseException {
        String mdsUrl = "https://mds3.fido.tools/execute/90c8ec276023503f7cd6ba188410dd38161d5ee14f4eb97b98a7f4b0c76482dd";

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