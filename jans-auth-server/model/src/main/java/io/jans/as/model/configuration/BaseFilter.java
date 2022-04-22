/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.model.configuration;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;

/**
 * @author Yuriy Movchan
 * @author Yuriy Zabrovarnyy
 * @author Javier Rojas Blum
 * @version April 13, 2016
 */
@XmlAccessorType(XmlAccessType.FIELD)
public class BaseFilter {

    @XmlElement(name = "filter", required = true)
    private String filter;

    @XmlElement(name = "bind", required = false)
    private Boolean bind;

    @XmlElement(name = "bind-password-attribute", required = false)
    private String bindPasswordAttribute;

    @XmlElement(name = "base-dn", required = true)
    private String baseDn;

    public String getFilter() {
        return filter;
    }

    public void setFilter(String filter) {
        this.filter = filter;
    }

    public Boolean getBind() {
        return bind;
    }

    public void setBind(Boolean bind) {
        this.bind = bind;
    }

    public String getBaseDn() {
        return baseDn;
    }

    public void setBaseDn(String baseDn) {
        this.baseDn = baseDn;
    }

    public String getBindPasswordAttribute() {
        return bindPasswordAttribute;
    }

    public void setBindPasswordAttribute(String bindPasswordAttribute) {
        this.bindPasswordAttribute = bindPasswordAttribute;
    }

    @Override
    public String toString() {
        return String.format("BaseFilter [filter=%s, bind=%s, bindPasswordAttribute=%s, baseDn=%s]", filter, bind, (bindPasswordAttribute == null ? null : "not_null"), baseDn);
    }

}
