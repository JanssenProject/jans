package io.jans.casa.plugins.authnmethod.service.otp;

import com.bastiaanjansen.otp.HMACAlgorithm;
import com.bastiaanjansen.otp.TOTPGenerator;
import com.google.common.io.BaseEncoding;
import io.jans.casa.misc.Utils;
import io.jans.casa.plugins.authnmethod.conf.otp.TOTPConfig;
import org.slf4j.Logger;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.net.URI;
import java.time.Duration;
import java.util.Base64;

/**
 * Created by jgomer on 2018-06-28.
 * An app. scoped bean that encapsulates logic related to generating and validating OTP keys.
 * See https://tools.ietf.org/html/rfc6238 and https://tools.ietf.org/html/rfc4226.
 * Migrated from com.lochbridge.oath to com.github.bastiaanjansen:otp-java.
 */
@ApplicationScoped
public class TOTPAlgorithmService implements IOTPAlgorithm {

    @Inject
    private Logger logger;

    private TOTPConfig conf;

    private String issuer;

    private HMACAlgorithm hmacShaAlgorithm;

    public void init(TOTPConfig conf, String issuer) {
        this.issuer = issuer;
        this.conf = conf;
        // conf value is e.g. "SHA1" / "SHA256" / "SHA512"
        hmacShaAlgorithm = HMACAlgorithm.valueOf(conf.getHmacShaAlgorithm().toUpperCase());
    }

    public byte[] generateSecretKey() {
        return Utils.randomBytes(conf.getKeyLength());
    }

    private TOTPGenerator buildGenerator(byte[] secret) {
        return new TOTPGenerator.Builder(BaseEncoding.base32().omitPadding().encode(secret))
                .withHOTPGenerator(builder -> {
                    builder.withPasswordLength(conf.getDigits());
                    builder.withAlgorithm(hmacShaAlgorithm);
                })
                .withPeriod(Duration.ofSeconds(conf.getTimeStep()))
                .build();
    }

    public String generateSecretKeyUri(byte[] secretKey, String displayName) {
        logger.trace("Generating secret key URI");
        try {
            // getURI base32-encodes the secret in the otpauth:// URI, as scanners expect.
            URI uri = buildGenerator(secretKey).getURI(issuer, displayName.replace(':', ' '));
            return uri.toString();
        } catch (Exception e) {
            throw new IllegalStateException("Could not build TOTP key URI", e);
        }
    }

    public String getExternalUid(String secretKey, String code) {
        return validateKey(secretKey, code)
                ? String.format("%s:%s", OTPType.TOTP.getName().toLowerCase(), secretKey)
                : null;
    }

    public boolean validateKey(String secretKey, String otpCode) {
        byte[] bsecret = Base64.getUrlDecoder().decode(secretKey);
        return buildGenerator(bsecret).verify(otpCode);
    }

}
