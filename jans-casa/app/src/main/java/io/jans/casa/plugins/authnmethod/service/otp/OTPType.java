package io.jans.casa.plugins.authnmethod.service.otp;

/**
 * One-time password algorithm type. Replaces
 * {@code com.lochbridge.oath.otp.keyprovisioning.OTPKey.OTPType} after the
 * migration to {@code com.github.bastiaanjansen:otp-java}, which does not ship
 * an equivalent enum.
 */
public enum OTPType {

    HOTP,
    TOTP;

    public String getName() {
        return name();
    }

}
