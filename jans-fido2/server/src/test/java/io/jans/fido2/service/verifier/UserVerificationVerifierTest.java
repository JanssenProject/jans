package io.jans.fido2.service.verifier;

import io.jans.fido2.exception.Fido2RuntimeException;
import io.jans.fido2.model.auth.AuthData;
import io.jans.orm.model.fido2.UserVerification;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserVerificationVerifierTest {

    @InjectMocks
    private UserVerificationVerifier userVerificationVerifier;

    @Mock
    private Logger log;

    @Test
    void verifyUserPresent_ifAuthDataFlagsIsEqual1_true() {
        AuthData authData = mock(AuthData.class);
        when(authData.getFlags()).thenReturn(new byte[]{1});

        boolean response = userVerificationVerifier.verifyUserPresent(authData);
        assertTrue(response);
    }

    @Test
    void verifyUserPresent_ifAuthDataFlagsNotEqual1_fido2RuntimeException() {
        AuthData authData = mock(AuthData.class);
        when(authData.getFlags()).thenReturn(new byte[]{0});

        Fido2RuntimeException ex = assertThrows(Fido2RuntimeException.class, () -> userVerificationVerifier.verifyUserPresent(authData));
        assertNotNull(ex);
        assertEquals(ex.getMessage(), "User not present");
    }

    @Test
    void verifyUserVerificationOption_ifUserVerificationIsRequired_valid() {
        AuthData authData = mock(AuthData.class);
        UserVerification userVerification = UserVerification.required;
        when(authData.getFlags()).thenReturn(new byte[]{4});

        userVerificationVerifier.verifyUserVerificationOption(userVerification, authData);
        verify(log).debug(contains("Required user present"), anyString());
    }

    @Test
    void verifyUserVerificationOption_ifUserVerificationIsPreferred_valid() {
        AuthData authData = mock(AuthData.class);
        UserVerification userVerification = UserVerification.preferred;
        when(authData.getFlags()).thenReturn(new byte[]{4});

        userVerificationVerifier.verifyUserVerificationOption(userVerification, authData);
        verify(log).debug(contains("Preferred user present"), anyString());
    }

    @Test
    void verifyUserVerificationOption_ifUserVerificationIsDiscouraged_valid() {
        AuthData authData = mock(AuthData.class);
        UserVerification userVerification = UserVerification.discouraged;
        when(authData.getFlags()).thenReturn(new byte[]{4});

        userVerificationVerifier.verifyUserVerificationOption(userVerification, authData);
        verify(log).debug(contains("Discouraged user present"), anyString());
    }

    @Test
    void verifyRequiredUserPresent_ifIsUserVerifiedIsFalse_fido2RuntimeException() {
        AuthData authData = mock(AuthData.class);
        when(authData.getFlags()).thenReturn(new byte[]{0});

        Fido2RuntimeException ex = assertThrows(Fido2RuntimeException.class, () -> userVerificationVerifier.verifyRequiredUserPresent(authData));
        assertNotNull(ex);
        assertEquals(ex.getMessage(), "User required is not present");
    }

    @Test
    void verifyRequiredUserPresent_ifIsUserVerifiedIsTrue_valid() {
        AuthData authData = mock(AuthData.class);
        when(authData.getFlags()).thenReturn(new byte[]{4});

        userVerificationVerifier.verifyRequiredUserPresent(authData);
        verify(log).debug(contains("Required user present"), anyString());
    }

    @Test
    void verifyPreferredUserPresent_validValues_valid() {
        AuthData authData = mock(AuthData.class);
        when(authData.getFlags()).thenReturn(new byte[]{0});

        userVerificationVerifier.verifyPreferredUserPresent(authData);
        verify(log).debug(contains("Preferred user present"), anyString());
    }

    @Test
    void verifyDiscouragedUserPresent_validValues_valid() {
        AuthData authData = mock(AuthData.class);
        when(authData.getFlags()).thenReturn(new byte[]{0});

        userVerificationVerifier.verifyDiscouragedUserPresent(authData);
        verify(log).debug(contains("Discouraged user present"), anyString());
    }
}
