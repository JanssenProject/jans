package io.jans.model.custom.script.type.token;

import io.jans.model.user.SimpleUser;

/**
 * @author Yuriy Z
 */
public class ScriptTokenExchangeControl {

    private boolean ok;

    private boolean skipBuiltinValidation;

    // user which will be associated with this token exchange grant
    // must point to io.jans.as.common.model.common.User - internal restriction
    private SimpleUser user;

    public ScriptTokenExchangeControl(boolean ok) {
        this.ok = ok;
    }

    public static ScriptTokenExchangeControl ok() {
        return new ScriptTokenExchangeControl(true);
    }

    public static ScriptTokenExchangeControl fail() {
        return new ScriptTokenExchangeControl(false);
    }

    public boolean isOk() {
        return ok;
    }

    public ScriptTokenExchangeControl setOk(boolean ok) {
        this.ok = ok;
        return this;
    }

    public boolean isSkipBuiltinValidation() {
        return skipBuiltinValidation;
    }

    public ScriptTokenExchangeControl setSkipBuiltinValidation(boolean skipBuiltinValidation) {
        this.skipBuiltinValidation = skipBuiltinValidation;
        return this;
    }

    public SimpleUser getUser() {
        return user;
    }

    public ScriptTokenExchangeControl setUser(SimpleUser user) {
        this.user = user;
        return this;
    }

    @Override
    public String toString() {
        return "ScriptTokenExchangeControl{" +
                "ok=" + ok +
                ", skipBuiltinValidation=" + skipBuiltinValidation +
                ", user=" + user +
                '}';
    }
}
