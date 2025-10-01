package io.jans.casa.plugins.consent;

import io.jans.casa.core.model.Client;
import io.jans.casa.core.model.Scope;
import io.jans.casa.plugins.consent.service.ClientAuthorizationsService;
import io.jans.casa.core.pojo.User;
import io.jans.casa.service.ISessionContext;
import io.jans.casa.ui.UIUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zkoss.bind.BindUtils;
import org.zkoss.bind.annotation.Init;
import org.zkoss.util.resource.Labels;
import org.zkoss.zk.ui.select.annotation.WireVariable;
import org.zkoss.zul.Messagebox;

import java.util.*;
import java.util.stream.Collectors;

/**
 * ZK ViewModel class for consent management page
 * @author jgomer
 */
public class AuthorizedClientsVM {

    private Logger logger = LoggerFactory.getLogger(getClass());

    @WireVariable
    private ISessionContext sessionContext;

    private User user;
    private ClientAuthorizationsService caService;
    private Map<Client, Set<Scope>> clients;

    public Map<Client, Set<Scope>> getClients() {
        return clients;
    }

    @Init
    public void init() {
        logger.info("Authorized Clients ViewModel inited");
        user = sessionContext.getLoggedUser();
        caService = new ClientAuthorizationsService();
        reloadClients();
    }

    public String getContactEmailsAsCSV(Client client) {
        return client.getContacts().stream().collect(Collectors.joining(", "));
    }

    public void revokeAll() {

        Messagebox.show(Labels.getLabel("clients.authorized.remove_hint_all"), null, Messagebox.YES | Messagebox.NO, Messagebox.QUESTION,
                event -> {
                    if (Messagebox.ON_YES.equals(event.getName())) {
                        logger.info("Removing all client authorizations for user {}", user.getId());

                        clients.keySet().forEach(client -> caService.removeClientAuthorizations(user.getId(), user.getUserName(), client.getInum()));
                        reloadClients();

                        if (clients.size() == 0) {
                            UIUtils.showMessageUI(true);
                        } else {
                            String detail = Labels.getLabel("clients.authorized.notall_removed");
                            UIUtils.showMessageUI(false, Labels.getLabel("general.error.detailed", new String[]{detail}));
                        }
                        //trigger refresh (this method is asynchronous...)
                        BindUtils.postNotifyChange(AuthorizedClientsVM.this, "clients");
                    }
                });

    }

    public void revoke(String clientId, String clientName) {

        Messagebox.show(Labels.getLabel("clients.authorized.remove_hint"), null, Messagebox.YES | Messagebox.NO, Messagebox.QUESTION,
                event -> {
                    if (Messagebox.ON_YES.equals(event.getName())) {

                        int currSize = clients.size();
                        caService.removeClientAuthorizations(user.getId(), user.getUserName(), clientId);
                        reloadClients();

                        if (currSize - 1 == clients.size()) {
                            UIUtils.showMessageUI(true);
                        } else {
                            //Removal failed
                            UIUtils.showMessageUI(false, Labels.getLabel("clients.authorized.remove_error", new String[]{clientName}));
                        }
                        //trigger refresh (this method is asynchronous...)
                        BindUtils.postNotifyChange(AuthorizedClientsVM.this, "clients");
                    }
                });

    }

    private void reloadClients() {
        logger.info("Recomputing client list associated to current user");
        //Get the clients associated to the current logged user and sort them by name
        clients = new TreeMap<>(Comparator.comparing(Client::getDisplayName));
        clients.putAll(caService.getUserClientPermissions(user.getId()));
    }

}
