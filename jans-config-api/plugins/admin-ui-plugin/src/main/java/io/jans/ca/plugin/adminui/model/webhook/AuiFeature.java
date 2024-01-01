package io.jans.ca.plugin.adminui.model.webhook;

import io.jans.orm.annotation.AttributeName;
import io.jans.orm.annotation.DN;
import io.jans.orm.annotation.DataEntry;
import io.jans.orm.annotation.ObjectClass;
import io.jans.orm.model.base.Entry;
import org.python.google.common.collect.Lists;
import org.python.google.common.collect.Sets;

import java.io.Serializable;
import java.util.List;
import java.util.Set;

@DataEntry(sortBy = { "auiFeatureId" })
@ObjectClass(value = "auiFeatures")
public class AuiFeature extends Entry implements Serializable {

    @AttributeName(name = "auiFeatureId")
    private String auiFeatureId;
    @AttributeName(name = "displayName")
    private String displayName;
    @AttributeName(name = "jansScope")
    private String jansScope;
    @AttributeName(name = "webhookId")
    private List<String> webhookIdsMapped;

    public String getAuiFeatureId() {
        return auiFeatureId;
    }

    public void setAuiFeatureId(String auiFeatureId) {
        this.auiFeatureId = auiFeatureId;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getJansScope() {
        return jansScope;
    }

    public void setJansScope(String jansScope) {
        this.jansScope = jansScope;
    }

    public List<String> getWebhookIdsMapped() {
        return webhookIdsMapped;
    }

    public void setWebhookIdsMapped(List<String> webhookIdsMapped) {
        if(webhookIdsMapped != null) {
            this.webhookIdsMapped = Lists.newArrayList(Sets.newHashSet(webhookIdsMapped));
        }
    }

    @Override
    public String toString() {
        return "AuiFeature{" +
                "auiFeatureId='" + auiFeatureId + '\'' +
                ", displayName='" + displayName + '\'' +
                ", jansScope='" + jansScope + '\'' +
                ", webhookIdsMapped=" + webhookIdsMapped +
                '}';
    }
}
