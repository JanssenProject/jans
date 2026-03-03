package io.jans.as.server.rate;

import jakarta.servlet.http.HttpServletRequest;

import java.io.IOException;

public class RateLimitContext {

    private final HttpServletRequest request;
    private final boolean rateLoggingEnabled;
    private CachedBodyHttpServletRequest cachedRequest;

    public RateLimitContext(HttpServletRequest request, boolean rateLoggingEnabled) {
        this.request = request;
        this.rateLoggingEnabled = rateLoggingEnabled;
    }

    public HttpServletRequest getRequest() {
        return request;
    }

    public boolean isRateLoggingEnabled() {
        return rateLoggingEnabled;
    }

    public boolean isCachedRequestAvailable() {
        return cachedRequest != null;
    }

    public CachedBodyHttpServletRequest getCachedRequest() throws IOException {
        if (cachedRequest == null) {
            cachedRequest = new CachedBodyHttpServletRequest(request);
        }
        return cachedRequest;
    }

    public void setCachedRequest(CachedBodyHttpServletRequest cachedRequest) {
        this.cachedRequest = cachedRequest;
    }
}
