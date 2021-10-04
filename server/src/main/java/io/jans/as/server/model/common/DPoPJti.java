package io.jans.as.server.model.common;

import java.io.Serializable;

public class DPoPJti implements Serializable {

    private String jti;
    private Long iat;
    private String htu;

    public DPoPJti(String jti, Long iat, String htu) {
        this.jti = jti;
        this.iat = iat;
        this.htu = htu;
    }

    public String getJti() {
        return jti;
    }

    public void setJti(String jti) {
        this.jti = jti;
    }

    public Long getIat() {
        return iat;
    }

    public void setIat(Long iat) {
        this.iat = iat;
    }

    public String getHtu() {
        return htu;
    }

    public void setHtu(String htu) {
        this.htu = htu;
    }
}
