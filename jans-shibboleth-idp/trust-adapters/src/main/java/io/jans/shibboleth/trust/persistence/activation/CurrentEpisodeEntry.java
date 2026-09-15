package io.jans.shibboleth.trust.persistence.activation;

import io.jans.orm.annotation.AttributeName;
import io.jans.orm.annotation.DataEntry;
import io.jans.orm.annotation.ObjectClass;
import io.jans.orm.model.base.BaseEntry;

/**
 * jans-orm storage entry for the current-episode pointer: at most one per trust relationship, keyed by the
 * trust-relationship id. Object class {@code jansTrustActivationEpisode}, under
 * {@code ou=trustActivationEpisodes,o=jans}; DN {@code inum=<trId>,ou=trustActivationEpisodes,o=jans}.
 * {@code jansWorkItemRef} names the current work item.
 */
@DataEntry
@ObjectClass("jansTrustActivationEpisode")
public class CurrentEpisodeEntry extends BaseEntry {

    private static final long serialVersionUID = 1L;

    @AttributeName(name = "inum", ignoreDuringUpdate = true)
    private String inum;

    @AttributeName(name = "jansWorkItemRef")
    private String workItemRef;

    public String getInum() {

        return inum;
    }

    public void setInum(String inum) {

        this.inum = inum;
    }

    public String getWorkItemRef() {

        return workItemRef;
    }

    public void setWorkItemRef(String workItemRef) {

        this.workItemRef = workItemRef;
    }
}
