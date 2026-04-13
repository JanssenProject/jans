package io.jans.shibboleth.model.error;


public class InterceptorFlowError extends TrustError {

    private InterceptorFlowError(String message) {
        super(message);
    }

    public static InterceptorFlowError cannotBeNullorBlank() {

        return new InterceptorFlowError("Interceptor flow is required and cannot be  blank");
    }
}