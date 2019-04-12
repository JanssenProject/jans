/*
 * oxCore is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.gluu.config.oxtrust;

import org.gluu.persist.model.base.Entry;
import org.gluu.persist.annotation.AttributeName;
import org.gluu.persist.annotation.DN;
import org.gluu.persist.annotation.DataEntry;
import org.gluu.persist.annotation.JsonObject;
import org.gluu.persist.annotation.ObjectClass;

/**
 * @author Yuriy Movchan
 * @version 0.9, 05/16/2013
 */
@DataEntry
@ObjectClass(values = { "top", "oxTrustConfiguration" })
public class LdapOxTrustConfiguration extends Entry {

    private static final long serialVersionUID = -15289347651306279L;

    @DN
    private String dn;

    @JsonObject
    @AttributeName(name = "oxTrustConfApplication")
    private AppConfiguration application;

    @JsonObject
    @AttributeName(name = "oxTrustConfCacheRefresh")
    private CacheRefreshConfiguration cacheRefresh;

    @AttributeName(name = "oxRevision")
    private long revision;

    @JsonObject
    @AttributeName(name = "oxTrustConfImportPerson")
    private ImportPersonConfig importPersonConfig;

    @JsonObject
    @AttributeName(name = "oxTrustConfAttributeResolver")
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
