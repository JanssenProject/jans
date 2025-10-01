package io.jans.casa.core.pojo;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * The superclass for all type of credentials
 * @author jgomer
 */
public class RegisteredCredential {

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private String nickName;

    public String getNickName() {
        return nickName;
    }

    public void setNickName(String nickname) {
        this.nickName = nickname;
    }

}
