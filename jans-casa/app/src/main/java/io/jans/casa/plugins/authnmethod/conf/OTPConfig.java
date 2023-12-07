package io.jans.casa.plugins.authnmethod.conf;

import com.fasterxml.jackson.core.type.TypeReference;
import io.jans.casa.misc.Utils;
import io.jans.casa.plugins.authnmethod.conf.otp.HOTPConfig;
import io.jans.casa.plugins.authnmethod.conf.otp.TOTPConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;

/**
 * POJO storing values needed for HOTP/TOTP. Static methods of this class parse information belonging to the OTP custom
 * script to be able to get an instance of this class
 * @author jgomer
 */
public class OTPConfig extends QRConfig {

    private static Logger LOGGER = LoggerFactory.getLogger(OTPConfig.class);

    private String issuer;
    private HOTPConfig hotp;
    private TOTPConfig totp;

    public String getIssuer() {
        return issuer;
    }

    public HOTPConfig getHotp() {
        return hotp;
    }

    public TOTPConfig getTotp() {
        return totp;
    }

    public void setIssuer(String issuer) {
        this.issuer = issuer;
    }

    public void setHotp(HOTPConfig hotp) {
        this.hotp = hotp;
    }

    public void setTotp(TOTPConfig totp) {
        this.totp = totp;
    }

    /**
     * Creates an OTPConfig object to hold all properties required for OTP key generation and QR code display
     * @param propsMap A map of string-ed key/value pairs with the source of data for this operation
     * @return null if an error or inconsistency is found while inspecting the configuration properties of the custom script.
     * Otherwise returns an OTPConfig object
     */
    public static OTPConfig get(Map<String, String> propsMap) {

        OTPConfig config = new OTPConfig();
        try {
            config.populate(propsMap);
            config.setIssuer(propsMap.get("issuer"));

            //Find the property of the script related to json file
            String jsonFile = propsMap.get("otp_conf_file");

            if (Utils.isNotEmpty(jsonFile)) {
                String contents = new String(Files.readAllBytes(Paths.get(jsonFile)), StandardCharsets.UTF_8);
                //Gluu Server 3.1.5 and earlier have a typo in default /etc/certs/otp_configuration.json, the following accommodates
                contents = contents.replaceFirst("\"htop\"", "\"hotp\"");

                String json = MAPPER.readTree(contents).get("hotp").toString();
                config.setHotp(MAPPER.readValue(json, new TypeReference<HOTPConfig>(){ }));

                json = MAPPER.readTree(contents).get("totp").toString();
                config.setTotp(MAPPER.readValue(json, new TypeReference<TOTPConfig>(){ }));
            } else {
                LOGGER.error("Property 'otp_conf_file' not found");
                config = null;
            }
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
            config = null;
        }
        return config;

    }

}
