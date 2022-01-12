/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.config.oxtrust;

import io.jans.orm.annotation.AttributeName;
import io.jans.orm.annotation.DN;
import io.jans.orm.annotation.DataEntry;
import io.jans.orm.annotation.JsonObject;
import io.jans.orm.annotation.ObjectClass;
import io.jans.orm.model.base.Entry;

/**
 * @author Yuriy Movchan
 * @version 0.9, 05/16/2013
 */
@DataEntry
@ObjectClass(value = "jansAppConf")
public class LdapOxTrustConfiguration extends Entry {

    private static final long serialVersionUID = -15289347651306279L;

    @DN
    private String dn;

    @JsonObject
    @AttributeName(name = "jansConfDyn")
    private AppConfiguration application;

    @JsonObject
    @AttributeName(name = "jansConfCacheRefresh")
    private CacheRefreshConfiguration cacheRefresh;

    @AttributeName(name = "oxRevision")
    private long revision;

    @JsonObject
    @AttributeName(name = "jansConfImportPerson")
    private ImportPersonConfig importPersonConfig;

    @JsonObject
    @AttributeName(name = "jansConfAttrResolver")
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

    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("LdapOxTrustConfiguration [dn=").append(dn).append(", application=").append(application).append(", cacheRefresh=")
                .append(cacheRefresh).append(", revision=").append(revision).append("]");
        return builder.toString();
    }

}
