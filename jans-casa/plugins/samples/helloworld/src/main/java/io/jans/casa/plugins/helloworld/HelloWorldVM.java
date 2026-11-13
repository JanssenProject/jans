package io.jans.casa.plugins.helloworld;

import io.jans.casa.misc.Utils;
import io.jans.casa.service.IPersistenceService;
import io.jans.casa.service.ISessionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zkoss.bind.annotation.Init;
import org.zkoss.bind.annotation.NotifyChange;

/**
 * A ZK <a href="http://books.zkoss.org/zk-mvvm-book/8.0/viewmodel/index.html" target="_blank">ViewModel</a> that acts
 * as the "controller" of page <code>index.zul</code> in this sample plugin. See <code>viewModel</code> attribute of
 * panel component of <code>index.zul</code>.
 * @author jgomer
 */
public class HelloWorldVM {

    private Logger logger = LoggerFactory.getLogger(getClass());

    private String message;
    private String organizationName;
    private IPersistenceService persistenceService;
    private ISessionContext sessionContext;

    /**
     * Getter of private class field <code>organizationName</code>.
     * @return A string with the value of the organization name found in your Gluu installation.
     */
    public String getOrganizationName() {
        return organizationName;
    }

    /**
     * Getter of private class field <code>message</code>.
     * @return A string value
     */
    public String getMessage() {
        return message;
    }

    /**
     * Setter of private class field <code>message</code>.
     * @param message A string with the contents typed in text box of page index.zul
     */
    public void setMessage(String message) {
        this.message = message;
    }

    /**
     * Initialization method for this ViewModel.
     */
    @Init
    public void init() {
        logger.info("Hello World ViewModel inited");
        persistenceService = Utils.managedBean(IPersistenceService.class);

        sessionContext = Utils.managedBean(ISessionContext.class);
        if (sessionContext.getLoggedUser() != null) {
            logger.info("There is a user logged in!");
        }

    }

    /**
     * The method called when the button on page <code>index.zul</code> is pressed. It sets the value for
     * <code>organizationName</code>.
     */
    @NotifyChange("organizationName")
    public void loadOrgName() {
        logger.debug("You typed {}", message);
        organizationName = persistenceService.getOrganization().getDisplayName();
    }

}
