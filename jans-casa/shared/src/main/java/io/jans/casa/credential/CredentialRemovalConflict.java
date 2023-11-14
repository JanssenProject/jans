package io.jans.casa.credential;

/**
 * Enumerates possible hypothetical scenarios that would arise in case a user's credential is dropped.
 */
public enum CredentialRemovalConflict {
    /**
     * It represents the case under which a removal would violate the constraint of having a minimum number of credentials
     * enrolled so 2FA can take place.
     */
    CREDS2FA_NUMBER_UNDERFLOW,
    /**
     * It represents the case under which a removal would violate the constraint of having at least one credential
     * belonging to certain types (as when using <code>2fa_requisite</code> in custom scripts, see the "About 2FA" page
     * in Casa administration guide.
     */
    REQUISITE_NOT_FULFILED;
}
