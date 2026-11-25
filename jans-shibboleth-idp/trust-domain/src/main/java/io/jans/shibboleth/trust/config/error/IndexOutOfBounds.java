package io.jans.shibboleth.trust.config.error;


public class IndexOutOfBounds extends TrustError {
    
    private IndexOutOfBounds(String message) {

        super(message);
    }

    public static IndexOutOfBounds forAssertionConsumerService(int index) {

        String message = String.format("Assertion consumer service index out of bounds (%d). Acceptable range 0-65535",index);
        return new IndexOutOfBounds(message);
    }
}
