package io.jans.inbound;

public class OpenIdParams {

    private String host;
    private boolean useDCR;
    private boolean useCachedClient;
    
    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public boolean isUseDCR() {
        return useDCR;
    }

    public void setUseDCR(boolean useDCR) {
        this.useDCR = useDCR;
    }

    public boolean isUseCachedClient() {
        return useCachedClient;
    }

    public void setUseCachedClient(boolean useCachedClient) {
        this.useCachedClient = useCachedClient;
    }
    
}
