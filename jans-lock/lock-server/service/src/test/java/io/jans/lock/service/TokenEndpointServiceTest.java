/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.lock.service;

import io.jans.as.client.TokenClient;
import io.jans.as.client.TokenResponse;
import io.jans.as.model.uma.wrapper.Token;
import io.jans.lock.model.AuditEndpointType;
import io.jans.lock.model.config.AppConfiguration;
import io.jans.service.EncryptionService;
import org.apache.http.HttpStatus;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.slf4j.Logger;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.testng.Assert.*;

/**
 * Unit tests for TokenEndpointService
 * 
 * @author Janssen Project
 */
public class TokenEndpointServiceTest {

    @Mock
    private Logger log;

    @Mock
    private AppConfiguration appConfiguration;

    @Mock
    private OpenIdService openIdService;

    @Mock
    private EncryptionService encryptionService;

    @InjectMocks
    private TokenEndpointService tokenEndpointService;

    @BeforeMethod
    public void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void testGetDecryptedPassword_withValidPassword_shouldDecrypt() throws Exception {
        String encryptedPassword = "encrypted_password";
        String decryptedPassword = "decrypted_password";
        
        when(encryptionService.decrypt(encryptedPassword)).thenReturn(decryptedPassword);
        
        String result = tokenEndpointService.getDecryptedPassword(encryptedPassword);
        
        assertEquals(result, decryptedPassword);
        verify(encryptionService).decrypt(encryptedPassword);
    }

    @Test
    public void testGetDecryptedPassword_withNull_shouldReturnNull() {
        String result = tokenEndpointService.getDecryptedPassword(null);
        
        assertNull(result);
        verify(encryptionService, never()).decrypt(anyString());
    }

    @Test
    public void testGetDecryptedPassword_whenDecryptionFails_shouldReturnNull() throws Exception {
        String encryptedPassword = "encrypted_password";
        
        when(encryptionService.decrypt(encryptedPassword)).thenThrow(new Exception("Decryption failed"));
        
        String result = tokenEndpointService.getDecryptedPassword(encryptedPassword);
        
        assertNull(result);
    }

    @Test
    public void testGetDecryptedPassword_withEmptyString_shouldAttemptDecryption() throws Exception {
        String emptyPassword = "";
        
        when(encryptionService.decrypt(emptyPassword)).thenReturn("");
        
        String result = tokenEndpointService.getDecryptedPassword(emptyPassword);
        
        assertEquals(result, "");
    }
}
