package org.gluu.model;

/**
 * Hold IMAP Data configuration
 *
 * @author Shekhar L
 */

public class GluuIMAPData implements java.io.Serializable {

    /**
     *
     */
    private static final long serialVersionUID = 216941324769217751L;

    private String imaphost;

    private ImapPassword imapPassword;

    private String useSSL;

    private String imapusername;

    private String imapport;

    public String getImaphost() {
        return imaphost;
    }

    public void setImaphost(String imaphost) {
        this.imaphost = imaphost;
    }

    public ImapPassword getImapPassword() {
        return imapPassword;
    }

    public void setImapPassword(ImapPassword imapPassword) {
        this.imapPassword = imapPassword;
    }

    public String getUseSSL() {
        return useSSL;
    }

    public void setUseSSL(String useSSL) {
        this.useSSL = useSSL;
    }

    public String getImapusername() {
        return imapusername;
    }

    public void setImapusername(String imapusername) {
        this.imapusername = imapusername;
    }

    public String getImapport() {
        return imapport;
    }

    public void setImapport(String imapport) {
        this.imapport = imapport;
    }

}
