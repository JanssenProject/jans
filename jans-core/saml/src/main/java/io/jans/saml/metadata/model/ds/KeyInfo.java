package io.jans.saml.metadata.model.ds;

import java.util.ArrayList;
import java.util.List;

public class KeyInfo {

    private String id;
    private List<X509Data> datalist;

    public KeyInfo() {

        this.id = null;
        this.datalist = new ArrayList<>();
    }

    public String getId() {

        return this.id;
    }

    public void setId(final String id) {

        this.id = id;
    }

    public List<X509Data> getDatalist() {

        return this.datalist;
    }

    public void addData(final X509Data data) {

        this.datalist.add(data);
    }
}