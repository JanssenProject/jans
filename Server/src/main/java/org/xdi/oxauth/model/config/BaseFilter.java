package org.xdi.oxauth.model.config;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;

/**
 * @author Yuriy Movchan
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 02/08/2012
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
