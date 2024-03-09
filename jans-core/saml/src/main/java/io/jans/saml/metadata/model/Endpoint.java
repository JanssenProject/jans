package io.jans.saml.metadata.model;

public class Endpoint {

    private String binding;
    private String location;
    private String responseLocation;

    public Endpoint() {

        this.binding = null;
        this.location = null;
        this.responseLocation = null;
    }

    public String getBinding() {

        return this.binding; 
    }

    public void setBinding(final String binding) {

        this.binding = binding;
    }

    public String getLocation() {

        return this.location;
    }

    public void setLocation(final String location) {

        this.location = location;
    }

    public String getResponseLocation() {

        return this.responseLocation;
    }

    public void setResponseLocation(final String responseLocation) {

        this.responseLocation = responseLocation;
    }
}