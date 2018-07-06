package org.xdi.oxauth.model.common;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

import com.google.common.base.Preconditions;
import com.ocpsoft.pretty.faces.util.StringUtils;

/**
 * @author yuriyz
 */
public class SessionTokens implements Serializable {

    private String sessionDn;

    private Set<String> tokenHashes = new HashSet<String>();

    public SessionTokens(String sessionDn) {
        this.sessionDn = sessionDn;
    }


    public String getSessionDn() {
        return sessionDn;
    }

    public void setSessionDn(String sessionDn) {
        this.sessionDn = sessionDn;
    }

    public Set<String> getTokenHashes() {
        return tokenHashes;
    }

    public void setTokenHashes(Set<String> tokenHashes) {
        this.tokenHashes = tokenHashes;
    }

    public String cacheKey() {
        Preconditions.checkState(StringUtils.isNotBlank(sessionDn));
        return sessionDn + "_tokens";
    }

    @Override
    public String toString() {
        return "SessionTokens{" +
                "sessionDn='" + sessionDn + '\'' +
                ", tokenHashes=" + tokenHashes +
                '}';
    }
}