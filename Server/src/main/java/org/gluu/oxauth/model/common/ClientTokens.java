package org.gluu.oxauth.model.common;

import com.google.common.base.Preconditions;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

import org.gluu.util.StringHelper;

/**
 * @author yuriyz
 */
public class ClientTokens implements Serializable {

    private String clientId;

    private Set<String> tokenHashes = new HashSet<String>();

    public ClientTokens(String clientId) {
        this.clientId = clientId;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public Set<String> getTokenHashes() {
        return tokenHashes;
    }

    public void setTokenHashes(Set<String> tokenHashes) {
        this.tokenHashes = tokenHashes;
    }

    public String cacheKey() {
        Preconditions.checkState(StringHelper.isNotEmpty(clientId));
        return clientId + "_tokens";
    }

    @Override
    public String toString() {
        return "ClientTokens{" +
                "clientId='" + clientId + '\'' +
                ", tokenHashes=" + tokenHashes +
                '}';
    }
}
