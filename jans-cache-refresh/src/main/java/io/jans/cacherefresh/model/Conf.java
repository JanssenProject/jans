package io.jans.cacherefresh.model;

import io.jans.orm.annotation.*;

@DataEntry
@ObjectClass(value = "jansAppConf")
public class Conf {

    @DN
    protected String dn;

    @AttributeName(name = "jansRevision")
    protected long revision;

    public String getDn() {
        return dn;
    }

    public void setDn(String dn) {
        this.dn = dn;
    }

    public long getRevision() {
        return revision;
    }

    public void setRevision(long revision) {
        this.revision = revision;
    }

    @Override
    public String toString() {
        return "Conf [dn=" + dn +   ", revision=" + revision + "]";
    }
}