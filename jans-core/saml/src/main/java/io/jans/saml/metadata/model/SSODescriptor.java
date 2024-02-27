package io.jans.saml.metadata.model;

import java.util.ArrayList;
import java.util.List;

public class SSODescriptor extends RoleDescriptor  {

    private List<Endpoint> singleLogoutServices;
    private List<String> nameIDFormats;

    public  SSODescriptor() {

        this.singleLogoutServices = new ArrayList<Endpoint>();
        this.nameIDFormats = new ArrayList<String>();
    }

    public List<Endpoint> getSingleLogoutServices() {

        return this.singleLogoutServices;
    }
    
    public void addSingleLogoutService(final Endpoint service) {

        this.singleLogoutServices.add(service);
    }

    public List<String> getNameIDFormats() {

        return this.nameIDFormats;
    }

    public void addNameIDFormat(final String nameIDFormat) {

        this.nameIDFormats.add(nameIDFormat);
    }

    public void setNameIDFormats(final List<String> nameIDFormats) {

        this.nameIDFormats = nameIDFormats;
    }
}