package io.jans.casa.extension;

import io.jans.casa.credential.BasicCredential;

import java.util.List;

import org.pf4j.ExtensionPoint;

/**
 * This interface is an extension point that plugin developers can use to add/override authentication mechanisms exhibited in Casa
 */
public interface AuthnMethod extends ExtensionPoint {

    /**
     * The qualified name of the Agama flow associated to this authentication method
     * @return A String value
     */
    String getAcr();

    /**
     * The resource bundle key (present in file <code>zk-label.properties</code>) used for displaying the name of this authentication
     * method in Gluu Casa UI. As an example, this is the key that is looked up when rendering the list of authentication
     * mechanisms for the user to pick their 2FA preference.
     * @return A non-null String
     */
    String getUINameKey();

    /**
     * The resource bundle key (present in file <code>zk-label.properties</code>) used for displaying the panel title (header) of this
     * authentication method in the main user page of Gluu Casa.
     * @return A non-null String
     */
    String getPanelTitleKey();


    /**
     * The resource bundle key (present in file <code>zk-label.properties</code>) used for displaying the summary text
     * (body) of the panel corresponding to this authentication method in the main user page of Gluu Casa.
     * @return A non-null String
     */
    String getPanelTextKey();

    /**
     * The resource bundle key (present in file <code>zk-label.properties</code>) used for displaying the button label of
     * this authentication method shown in the main user page of Gluu Casa. This button takes the user to the specific
     * page where enrolling/listing of credentials is carried out. Normally, the label should look like "Add/Remove..."
     * @return A non-null String
     */
    String getPanelButtonKey();

    /**
     * The URL where users will be taken for enrolling/listing of credentials associated to this authentication method.
     * If the implementing class is part of a plugin, this will be interpreted relative to the base URL of the plugin
     * (e.g <code>/pl/PLUGIN-ID/</code>).
     * @return A string representing a relative URL. An empty String "" will work for pointing to index.zul or index.jsp
     */
    String getPageUrl();

    /**
     * The resource bundle key (present in file <code>zk-label.properties</code>) used for displaying auxiliary text in
     * the panel of this authentication method. Override this method only when you want to display a text, otherwise no
     * text will be added to panel.
     * @return A non-null string
     */
    default String getPanelBottomTextKey(){
        return null;
    }

    /**
     * A method invoked by Casa when a change in the configuration of the associated Agama flow is detected. This
     * allows developers to re-read configuration parameters part of the flow that may drive the behaviour
     * of the enrollment/listing functionalities or configure any other internal aspect which may result relevant when
     * configuration changes.
     */
    void reloadConfiguration();

    /**
     * Determines whether this authentication method should be treated as one of the potential methods users should
     * be presented for enrolling before trying to add any other credential in the system. If the method you are implementing
     * is highly accessible (does not entail any important hardware/software constraints), then it is a good candidate. In
     * that case, override this interface method and return true (actually this a default Java interface method that
     * returns false).
     * <p>Once a user has enrolled one credential belonging to any method of this kind, they can proceed with any other
     * form of enrollment and activate second factor authentication. The aim is to avoid users locking as much as
     * possible.</p>
     * @return A boolean value
     */
    default boolean mayBe2faActivationRequisite() {
        return false;
    }

    /**
     * Returns a <code>List</code> of {@link BasicCredential} instances representing the credentials enrolled by the
     * user (identified with the parameter supplied) for this specific authentication method. The list should account only
     * for credentials in a valid state for being displayed in the main user page of Gluu Casa. Because of the nature of
     * certain credential types or authentication methods, credentials may internally handle states (eg compromised, locked,
     * expired, etc.) where they are not deemed as valid.
     * @param id User ID (inum)
     * @return Credentials that will be displayed in the summary page of user's credentials
     */
    List<BasicCredential> getEnrolledCreds(String id);

    /**
     * The number of credentials enrolled by the user (identified with the parameter supplied) for this specific
     * authentication method. Only credentials in a valid state should be accounted. This method is not called when
     * rendering the summary page of user's credentials but has other internal uses. Ideally it should be implemented without
     * calling the <code>size</code> method of {@link #getEnrolledCreds(String)} but in a more efficient manner that however,
     * should yield the same integer value.
     * @param id User ID (inum)
     * @return An integer value (zero if no credentials)
     */
    int getTotalUserCreds(String id);

}
