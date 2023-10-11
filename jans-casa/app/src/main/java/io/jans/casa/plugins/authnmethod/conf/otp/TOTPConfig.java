package io.jans.casa.plugins.authnmethod.conf.otp;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Created by jgomer on 2018-06-28.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class TOTPConfig extends BaseOTPConfig {

    private int timeStep;
    private String hmacShaAlgorithm;

    public int getTimeStep() {
        return timeStep;
    }

    public String getHmacShaAlgorithm() {
        return hmacShaAlgorithm;
    }

    public void setTimeStep(int timeStep) {
        this.timeStep = timeStep;
    }

    public void setHmacShaAlgorithm(String hmacShaAlgorithm) {
        this.hmacShaAlgorithm = hmacShaAlgorithm;
    }

}
