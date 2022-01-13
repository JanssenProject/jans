/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.scim.model.fido2;

import java.io.Serializable;
import java.util.Date;

import io.jans.orm.model.base.BaseEntry;
import io.jans.orm.annotation.AttributeName;
import io.jans.orm.annotation.DataEntry;
import io.jans.orm.annotation.ObjectClass;

/**
 * Fido2 base persistence entry
 *
 * @author Yuriy Movchan
 * @version 11/02/2018
 */
@DataEntry(sortBy = "creationDate")
@ObjectClass
public class Fido2Entry extends BaseEntry implements Serializable {

	private static final long serialVersionUID = 7351459527571263266L;

	@AttributeName(ignoreDuringUpdate = true, name = "jansId")
    private String id;

    @AttributeName(ignoreDuringUpdate = true, name = "jansCodeChallenge")
    private String challenge;

    @AttributeName(ignoreDuringUpdate = true, name = "jansCodeChallengeHash")
    private String challengeHash;

    @AttributeName(ignoreDuringUpdate = true, name = "creationDate")
    private Date creationDate;

    @AttributeName(ignoreDuringUpdate = true, name = "jansSessStateId")
    private String sessionId;

    @AttributeName(name = "personInum")
    private String userInum;

    public Fido2Entry() {
    }

    public Fido2Entry(String dn) {
        super(dn);
    }

    public Fido2Entry(String dn, String id, Date creationDate, String sessionId, String userInum, String challenge) {
        super(dn);
        this.id = id;
        this.creationDate = creationDate;
        this.sessionId = sessionId;
        this.userInum = userInum;
        this.challenge = challenge;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getChallenge() {
        return challenge;
    }

    public void setChallenge(String challenge) {
        this.challenge = challenge;
    }

    public String getChallengeHash() {
        return challengeHash;
    }

    public void setChallengeHash(String challengeHash) {
        this.challengeHash = challengeHash;
    }

    public Date getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(Date creationDate) {
        this.creationDate = creationDate;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getUserInum() {
        return userInum;
    }

    public void setUserInum(String userInum) {
        this.userInum = userInum;
    }

}