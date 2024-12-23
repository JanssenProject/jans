package io.jans.fido2.service;

import io.jans.fido2.model.common.RelyingParty;
import io.jans.fido2.model.conf.AppConfiguration;
import io.jans.fido2.model.conf.Fido2Configuration;
import io.jans.fido2.model.conf.RequestedParty;
import io.jans.fido2.model.error.ErrorResponseFactory;
import io.jans.fido2.service.operation.AttestationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AttestationServiceTest {
    @InjectMocks
    private AttestationService attestationService;

    @Mock
    private AppConfiguration appConfiguration;

    @Mock
    private Logger log;

    @Mock
    private ErrorResponseFactory errorResponseFactory;


    @Test
    void createRpDomain_withValidIssuerAndDomain_createsRelyingPartySuccessfully() {
        String rpDomain = "my.jans.server";
        String rpId = "https://my.jans.server";
        Fido2Configuration fido2Config = mock(Fido2Configuration.class);

        when(appConfiguration.getIssuer()).thenReturn(rpId);
        when(appConfiguration.getFido2Configuration()).thenReturn(fido2Config);

        RelyingParty response = attestationService.createRpDomain(rpDomain);

        assertNotNull(response);
        assertEquals(rpDomain, response.getId());
        assertEquals(rpId, response.getName());

        verify(appConfiguration).getFido2Configuration();
        verify(appConfiguration).getIssuer();
        verifyNoInteractions(log, errorResponseFactory);
    }

    @Test
    void createRpDomain_ifRequestedPartiesContainsMatchingDomain_success() {
        String rpDomain = "my.jans.server";

        Fido2Configuration fido2Config = mock(Fido2Configuration.class);

        RequestedParty requestedParty = mock(RequestedParty.class);
        String requestedPartyId = "https://my.jans.server";
        String[] origins = {"my.jans.server",};
        when(requestedParty.getOrigins()).thenReturn(Arrays.asList(origins));
        when(requestedParty.getId()).thenReturn(requestedPartyId);

        List<RequestedParty> requestedParties = List.of(requestedParty);
        when(fido2Config.getRequestedParties()).thenReturn(requestedParties);

        when(appConfiguration.getFido2Configuration()).thenReturn(fido2Config);

        RelyingParty response = attestationService.createRpDomain(rpDomain);

        assertNotNull(response);
        assertEquals(rpDomain, response.getId());
        assertEquals(requestedPartyId, response.getName());

        verify(appConfiguration).getFido2Configuration();
        verify(fido2Config).getRequestedParties();
        verifyNoInteractions(log, errorResponseFactory);
    }

}
