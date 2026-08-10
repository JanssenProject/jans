package io.jans.fido2.service.verifier;

import io.jans.fido2.exception.Fido2RuntimeException;
import io.jans.fido2.model.auth.AuthData;
import io.jans.orm.model.fido2.UserVerification;
import org.junit.jupiter.api.BeforeEach;
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

    @BeforeEach
    void enableDebugLogging() {
        lenient().when(log.isDebugEnabled()).thenReturn(true);
    }

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
        assertEquals("User not present", ex.getMessage());
    }

    @Test
    void verifyUserPresent_ifUpBitNotSetButUvSet_rejected() {
        // CONF-06: User-Present must be enforced independently of UV. flags 0x04 = UV set, UP clear.
        AuthData authData = mock(AuthData.class);
        when(authData.getFlags()).thenReturn(new byte[]{4});

        Fido2RuntimeException ex = assertThrows(Fido2RuntimeException.class, () -> userVerificationVerifier.verifyUserPresent(authData));
        assertEquals("User not present", ex.getMessage());
    }

    @Test
    void verifyUserVerified_ifUvBitSet_true() {
        // CONF-08 regression guard: UV flag is 0x04. The old check (flags & 0x04) == 1 could never be
        // true; the corrected check (flags & 0x04) != 0 must return true when the UV bit is set.
        AuthData authData = mock(AuthData.class);
        when(authData.getFlags()).thenReturn(new byte[]{4});

        assertTrue(userVerificationVerifier.verifyUserVerified(authData));
    }

    @Test
    void verifyUserVerified_ifUvBitNotSet_false() {
        AuthData authData = mock(AuthData.class);
        when(authData.getFlags()).thenReturn(new byte[]{0});

        assertFalse(userVerificationVerifier.verifyUserVerified(authData));
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
        assertEquals("User required is not present", ex.getMessage());
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
