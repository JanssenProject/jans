/*
 * oxCore is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.gluu.config.oxtrust;

import org.gluu.persist.model.base.Entry;
import org.gluu.persistence.annotation.LdapAttribute;
import org.gluu.persistence.annotation.LdapDN;
import org.gluu.persistence.annotation.LdapEntry;
import org.gluu.persistence.annotation.LdapJsonObject;
import org.gluu.persistence.annotation.LdapObjectClass;

/**
 * @author Yuriy Movchan
 * @version 0.9, 05/16/2013
 */
@Entry
@ObjectClass(values = { "top", "oxTrustConfiguration" })
public class LdapOxTrustConfiguration extends Entry {

    private static final long serialVersionUID = -15289347651306279L;

    @DN
    private String dn;

    @JsonObject
    @Attribute(name = "oxTrustConfApplication")
    private AppConfiguration application;

    @JsonObject
    @Attribute(name = "oxTrustConfCacheRefresh")
    private CacheRefreshConfiguration cacheRefresh;

    @Attribute(name = "oxRevision")
    private long revision;

    @JsonObject
    @Attribute(name = "oxTrustConfImportPerson")
    private ImportPersonConfig importPersonConfig;

    @JsonObject
    @Attribute(name = "oxTrustConfAttributeResolver")
    private AttributeResolverConfiguration attributeResolverConfig;

    public LdapOxTrustConfiguration() {
    }

    public AppConfiguration getApplication() {
        return application;
    }

    public void setApplication(AppConfiguration application) {
        this.application = application;
    }

    public CacheRefreshConfiguration getCacheRefresh() {
        return cacheRefresh;
    }

    public void setCacheRefresh(CacheRefreshConfiguration cacheRefresh) {
        this.cacheRefresh = cacheRefresh;
    }

    public long getRevision() {
        return revision;
    }

    public void setRevision(long revision) {
        this.revision = revision;
    }

    // issue 102 - begin : changed by shekhar
    public ImportPersonConfig getImportPersonConfig() {
        return importPersonConfig;
    }

    public void setImportPersonConfig(ImportPersonConfig importPersonConfig) {
        this.importPersonConfig = importPersonConfig;
    } // issue 102 - end : changed by shekhar

    public AttributeResolverConfiguration getAttributeResolverConfig() {
        return attributeResolverConfig;
    }

    public void setAttributeResolverConfig(AttributeResolverConfiguration attributeResolverConfig) {
        this.attributeResolverConfig = attributeResolverConfig;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("LdapOxTrustConfiguration [dn=").append(dn).append(", application=").append(application).append(", cacheRefresh=")
                .append(cacheRefresh).append(", revision=").append(revision).append("]");
        return builder.toString();
    }

}
