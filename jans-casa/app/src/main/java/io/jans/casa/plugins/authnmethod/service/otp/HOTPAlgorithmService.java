package io.jans.casa.plugins.authnmethod.service.otp;

import com.google.common.io.BaseEncoding;
import com.lochbridge.oath.otp.HOTPValidationResult;
import com.lochbridge.oath.otp.HOTPValidator;
import com.lochbridge.oath.otp.keyprovisioning.OTPAuthURIBuilder;
import com.lochbridge.oath.otp.keyprovisioning.OTPKey;
import io.jans.casa.misc.Utils;
import io.jans.casa.plugins.authnmethod.conf.otp.HOTPConfig;
import org.slf4j.Logger;
import org.zkoss.util.Pair;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.Base64;

import static com.lochbridge.oath.otp.keyprovisioning.OTPKey.OTPType;

/**
 * Created by jgomer on 2018-06-28.
 * An app. scoped bean that encapsulates logic related to generating and validating OTP keys.
 * See https://tools.ietf.org/html/rfc6238 and https://tools.ietf.org/html/rfc4226.
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

    public String generateSecretKeyUri(byte[] secretKey, String displayName) {

        String secretKeyBase32 = BaseEncoding.base32().omitPadding().encode(secretKey);
        OTPKey otpKey = new OTPKey(secretKeyBase32, OTPType.HOTP);

        OTPAuthURIBuilder uribe = OTPAuthURIBuilder.fromKey(otpKey).label(displayName.replace(':', ' '));
        uribe = uribe.issuer(issuer).digits(conf.getDigits());

        logger.trace("Generating secret key URI");
        return uribe.build().toUriString();

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
        HOTPValidationResult result = HOTPValidator.lookAheadWindow(window).validate(bsecret, movingFactor, conf.getDigits(), otpCode);
        return result.isValid() ? new Pair<>(true, result.getNewMovingFactor()) : new Pair<>(false, null);   
    }
    
}
