package io.jans.casa.plugins.authnmethod.conf.otp;

/**
 * Created by jgomer on 2018-06-28.
 */
public class BaseOTPConfig {

    private int keyLength;
    private int digits;

    public int getKeyLength() {
        return keyLength;
    }

    public void setKeyLength(int keyLength) {
        this.keyLength = keyLength;
    }

    public int getDigits() {
        return digits;
    }

    public void setDigits(int digits) {
        this.digits = digits;
    }

}
