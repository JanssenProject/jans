package io.jans.casa.plugins.authnmethod.conf.otp;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Created by jgomer on 2018-06-28.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class HOTPConfig extends BaseOTPConfig {

    private int lookAheadWindow;

    public int getLookAheadWindow() {
        return lookAheadWindow;
    }

    public void setLookAheadWindow(int lookAheadWindow) {
        this.lookAheadWindow = lookAheadWindow;
    }

}
