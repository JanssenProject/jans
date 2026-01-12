package io.jans.configapi.core.model.adminui;

import io.jans.orm.annotation.AttributeName;
import io.jans.orm.annotation.DN;
import io.jans.orm.annotation.DataEntry;
import io.jans.orm.annotation.ObjectClass;
import java.util.Date;

@DataEntry
@ObjectClass(value = "adminUISession")
public class AdminUISession {
    @DN
    private String dn;
    @AttributeName(
            ignoreDuringUpdate = true
    )
    private String inum;
    @AttributeName(name = "sid")
    private String sessionId;
    @AttributeName(name = "jansUsrDN")
    private String jansUsrDN;
    @AttributeName(name = "jansUjwt")
    private String ujwt;
    @AttributeName(name = "creationDate")
    private Date creationDate = new Date();
    @AttributeName(name = "exp")
    private Date expirationDate;

    /**
     * Gets the distinguished name (DN) for this entry.
     *
     * @return the distinguished name (DN) of this data store entry
     */
    public String getDn() {
        return dn;
    }

    /**
     * Sets the distinguished name (DN) of this admin UI session entity.
     *
     * @param dn the distinguished name in the data store
     */
    public void setDn(String dn) {
        this.dn = dn;
    }

    /**
     * Gets the inum identifier for this AdminUISession.
     *
     * @return the inum identifier
     */
    public String getInum() {
        return inum;
    }

    /**
     * Set the inum (identifier) for this AdminUISession.
     *
     * @param inum the inum value to assign; typically an immutable identifier used by the persistence layer
     */
    public void setInum(String inum) {
        this.inum = inum;
    }

    /**
     * Session identifier for this admin UI session.
     *
     * @return the session identifier mapped to the `"sid"` attribute, or `null` if not set
     */
    public String getSessionId() {
        return sessionId;
    }

    /**
     * Sets the session identifier for this AdminUISession.
     *
     * @param sessionId the session identifier mapped to the "sid" attribute in the data store
     */
    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    /**
     * Gets the user JSON Web Token associated with this admin UI session.
     *
     * @return the user JWT string, or {@code null} if not set
     */
    public String getUjwt() {
        return ujwt;
    }

    /**
     * Sets the user JSON Web Token associated with this admin UI session.
     *
     * @param ujwt the user JWT string to associate with the session
     */
    public void setUjwt(String ujwt) {
        this.ujwt = ujwt;
    }

    /**
     * The session's creation timestamp.
     *
     * If not set explicitly, this value is initialized to the time the object was created.
     *
     * @return the creation timestamp of the session
     */
    public Date getCreationDate() {
        return creationDate;
    }

    /**
     * Sets the session's creation timestamp.
     *
     * @param creationDate the timestamp indicating when the admin UI session was created
     */
    public void setCreationDate(Date creationDate) {
        this.creationDate = creationDate;
    }

    /**
     * Gets the session's expiration timestamp.
     *
     * @return the expiration timestamp of the session, or {@code null} if not set
     */
    public Date getExpirationDate() {
        return expirationDate;
    }

    /**
     * Set the session's expiration timestamp.
     *
     * @param expirationDate the timestamp when the session expires
     */
    public void setExpirationDate(Date expirationDate) {
        this.expirationDate = expirationDate;
    }

    /**
     * User distinguished name associated with this admin UI session.
     *
     * @return the user's distinguished name (DN), or {@code null} if not set
     */
    public String getJansUsrDN() {
        return jansUsrDN;
    }

    /**
     * Sets the distinguished name (DN) of the admin UI user associated with this session.
     *
     * @param jansUsrDN the user's distinguished name to associate with the session
     */
    public void setJansUsrDN(String jansUsrDN) {
        this.jansUsrDN = jansUsrDN;
    }
}