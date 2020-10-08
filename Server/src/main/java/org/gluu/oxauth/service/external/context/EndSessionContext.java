package org.gluu.oxauth.service.external.context;

import com.google.common.collect.Sets;
import io.jans.model.custom.script.conf.CustomScriptConfiguration;
import org.gluu.oxauth.model.common.SessionId;

import javax.servlet.http.HttpServletRequest;
import java.util.Set;

/**
 * @author Yuriy Zabrovarnyy
 */
public class EndSessionContext extends ExternalScriptContext {

    private CustomScriptConfiguration script;
    private final Set<String> frontchannelLogoutUris;
    private final String postLogoutRedirectUri;
    private SessionId sessionId;

    public EndSessionContext(HttpServletRequest httpRequest, Set<String> frontchannelLogoutUris, String postLogoutRedirectUri, SessionId sessionId) {
        super(httpRequest);
        this.frontchannelLogoutUris = frontchannelLogoutUris;
        this.postLogoutRedirectUri = postLogoutRedirectUri;
        this.sessionId = sessionId;
    }

    public SessionId getSessionId() {
        return sessionId;
    }

    public CustomScriptConfiguration getScript() {
        return script;
    }

    public void setScript(CustomScriptConfiguration script) {
        this.script = script;
    }

    public Set<String> getFrontchannelLogoutUris() {
        return Sets.newHashSet(frontchannelLogoutUris);
    }

    public String getPostLogoutRedirectUri() {
        return postLogoutRedirectUri;
    }

    @Override
    public String toString() {
        return "EndSessionContext{" +
                "script=" + (script != null ? script.getName() : "") +
                ", frontchannelLogoutUris=" + frontchannelLogoutUris +
                ", postLogoutRedirectUri='" + postLogoutRedirectUri + '\'' +
                ", sessionId='" + sessionId + '\'' +
                "} " + super.toString();
    }
}
