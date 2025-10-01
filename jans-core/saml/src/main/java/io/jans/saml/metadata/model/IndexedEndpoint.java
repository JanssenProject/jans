package io.jans.saml.metadata.model;

public class IndexedEndpoint extends Endpoint {

    private Integer index;
    private Boolean isDefault;

    public IndexedEndpoint() {

        this.index = 0;
        this.isDefault = false;
    }

   
    public Integer getIndex() {

        return this.index;
    }

    public void setIndex(final Integer index) {

        this.index = index;
    }

    public Boolean getIsDefault() {

        return this.isDefault;
    }

    public void setIsDefault(final Boolean isDefault) {

        this.isDefault = isDefault;
    }
}