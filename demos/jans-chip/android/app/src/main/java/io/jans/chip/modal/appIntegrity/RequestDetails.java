package io.jans.chip.modal.appIntegrity;

public class RequestDetails{
    private String requestPackageName;
    private String timestampMillis;
    private String nonce;

    public String getRequestPackageName() {
        return requestPackageName;
    }

    public void setRequestPackageName(String requestPackageName) {
        this.requestPackageName = requestPackageName;
    }

    public String getTimestampMillis() {
        return timestampMillis;
    }

    public void setTimestampMillis(String timestampMillis) {
        this.timestampMillis = timestampMillis;
    }

    public String getNonce() {
        return nonce;
    }

    public void setNonce(String nonce) {
        this.nonce = nonce;
    }
}