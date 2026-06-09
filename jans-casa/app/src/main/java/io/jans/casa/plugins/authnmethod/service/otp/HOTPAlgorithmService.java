package io.jans.casa.plugins.authnmethod.service.otp;

import com.bastiaanjansen.otp.HMACAlgorithm;
import com.bastiaanjansen.otp.HOTPGenerator;
import com.google.common.io.BaseEncoding;
import io.jans.casa.misc.Utils;
import io.jans.casa.plugins.authnmethod.conf.otp.HOTPConfig;
import org.slf4j.Logger;
import org.zkoss.util.Pair;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.net.URI;
import java.util.Base64;

/**
 * Created by jgomer on 2018-06-28.
 * An app. scoped bean that encapsulates logic related to generating and validating OTP keys.
 * See https://tools.ietf.org/html/rfc6238 and https://tools.ietf.org/html/rfc4226.
 * Migrated from com.lochbridge.oath to com.github.bastiaanjansen:otp-java.
 */
@ApplicationScoped
public class HOTPAlgorithmService implements IOTPAlgorithm {

    private static int MAX_LOOK_AHEAD_WINDOW = 25;    //Yubico recommends a look-ahead window for authentication to be no more than 25

    @Inject
    private Logger logger;

    private HOTPConfig conf;

    private String issuer;

    public void init(HOTPConfig conf, String issuer) {
        this.issuer = issuer;
        this.conf = conf;
    }

    public byte[] generateSecretKey() {
        return Utils.randomBytes(conf.getKeyLength());
    }

    private HOTPGenerator buildGenerator(byte[] secret) {
        return new HOTPGenerator.Builder(BaseEncoding.base32().omitPadding().encode(secret))
                .withPasswordLength(conf.getDigits())
                .withAlgorithm(HMACAlgorithm.SHA1)
                .build();
    }

    public String generateSecretKeyUri(byte[] secretKey, String displayName) {
        logger.trace("Generating secret key URI");
        try {
            URI uri = buildGenerator(secretKey).getURI(0, issuer, displayName.replace(':', ' '));
            return uri.toString();
        } catch (Exception e) {
            throw new IllegalStateException("Could not build HOTP key URI", e);
        }
    }

    public String getExternalUid(String secretKey, String code) {
        Pair<Boolean, Long> result = validateKey(secretKey, code);
        return result.getX()
                ? String.format("%s:%s;%s", OTPType.HOTP.getName().toLowerCase(), secretKey, result.getY())
                : null;
    }

    private Pair<Boolean, Long> validateKey(String secretKey, String otpCode) {
        //Use 1 as moving factor (assumes this is the very first use of the OTP hard token). In practice, this might not
        //be the case, so a big value for look ahead window is used. This should not be done when validating an OTPs at
        //login time, however, for enrollment is OK
        return validateKey(secretKey, otpCode, 1, MAX_LOOK_AHEAD_WINDOW);
    }

    public Pair<Boolean, Long> validateKey(String secretKey, String otpCode, int movingFactor, Integer alternativeLookAheadWindow) {

        byte[] bsecret = Base64.getUrlDecoder().decode(secretKey);

        int window = alternativeLookAheadWindow == null ? conf.getLookAheadWindow() : alternativeLookAheadWindow;
        // otp-java's verify(code, counter) checks a single counter; iterate the look-ahead
        // window ourselves so we can return the matched counter as the new moving factor
        // (lochbridge's HOTPValidationResult.getNewMovingFactor()).
        HOTPGenerator generator = buildGenerator(bsecret);
        for (long counter = movingFactor; counter <= (long) movingFactor + window; counter++) {
            if (generator.verify(otpCode, counter)) {
                return new Pair<>(true, counter + 1);
            }
        }
        return new Pair<>(false, null);
    }

}
