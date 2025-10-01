package io.jans.casa.plugins.acctlinking.vm;

import io.jans.inbound.Provider;
import io.jans.casa.plugins.acctlinking.AccountsLinkingService;
import io.jans.casa.service.ISessionContext;
import io.jans.casa.ui.UIUtils;

import java.util.*;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zkoss.bind.BindUtils;
import org.zkoss.bind.annotation.*;
import org.zkoss.util.resource.Labels;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventQueues;
import org.zkoss.zk.ui.select.annotation.WireVariable;
import org.zkoss.zul.Messagebox;

public class AccountsLinkingVM {

    public static final String LINK_QUEUE = "social_queue";

    public static final String EVENT_NAME = "linked";
    
    public static final long ENROLL_TIME_MS = TimeUnit.MINUTES.toMillis(1);  //1 min

    private Logger logger = LoggerFactory.getLogger(getClass());

    @WireVariable
    private ISessionContext sessionContext;
    
    private AccountsLinkingService als;
    private Map<String, Provider> providers;
    private Map<String, String> accounts;

    private String userId;    
    private String pendingProvider;    
    private long pendingLinkingExpiresAt;
    
    public Map<String, Provider> getProviders() {
        return providers;
    }
    
    public Map<String, String> getAccounts() {
        return accounts;
    }
    
    public String getPendingProvider() {
        return pendingProvider;
    }
    
    public boolean isUsePopup() {
        return als.usePopup();
    }
    
    public AccountsLinkingVM() {
        als = AccountsLinkingService.getInstance();
    }
    
    @Init
    public void init() {
        
        try {
            logger.info("Refreshing list of identity providers");
            providers = als.getProviders(true);            
            logger.info("{} identity providers found", providers.size());
            
            userId = sessionContext.getLoggedUser().getId();
            parseLinkedAccounts();
            
            if (providers.size() > 0) {
    
                EventQueues.lookup(LINK_QUEUE, EventQueues.SESSION, true).subscribe(event -> {
                        if (event.getName().equals(EVENT_NAME)) {

                            String data = Optional.ofNullable(event.getData()).map(Object::toString).orElse(null);
                            if (data != null) {
                                logger.info("Received link start event for {}", data);
                                pendingLinkingExpiresAt = System.currentTimeMillis() + ENROLL_TIME_MS;
                                pendingProvider = data;
                            } else {
                                logger.info("Received linked event");
                                cancel();
                                parseLinkedAccounts();
                            }
                            BindUtils.postNotifyChange(AccountsLinkingVM.this, "accounts", "pendingProvider");
                        }
                });
            }
        
        } catch (Exception e) {
            logger.error(e.getMessage());
        }

    }

    @NotifyChange("pendingProvider")
    public void cancel() {
        pendingProvider = null;
    }

    @Command
    public void poll() {
        
        if (pendingProvider != null && pendingLinkingExpiresAt < System.currentTimeMillis()) {
            logger.info("Too much time elapsed for linking to finish");
            cancel();
            //I could have used @NotifyChange("pendingProvider") in this method but postnotify will give
            //better performance here. UI refresh takes place only if this IF is reached 
            BindUtils.postNotifyChange(AccountsLinkingVM.this, "pendingProvider");
        }

    }   
    
    public void remove(String providerId, String extUid) {        

        if (accounts.size() > 1 || als.hasPassword(userId)) {
            Provider p = providers.get(providerId);
            
            Messagebox.show(Labels.getLabel("al.remove_hint"), null, Messagebox.YES | Messagebox.NO, Messagebox.QUESTION,
                    event -> {
                        if (Messagebox.ON_YES.equals(event.getName())) {

                            if (als.delink(userId, providerId, extUid)) {
                                parseLinkedAccounts();
                                UIUtils.showMessageUI(true, Labels.getLabel("al.removed_link", new String[]{ p.getDisplayName() }));
                                BindUtils.postNotifyChange(AccountsLinkingVM.this, "accounts");
                            } else {
                                UIUtils.showMessageUI(false);
                            }
                        }
                    });
        } else {
            Messagebox.show(Labels.getLabel("al.linking_pass_needed"), null, Messagebox.OK, Messagebox.INFORMATION);
        }

    }
    
    private void parseLinkedAccounts() {
        logger.info("Parsing linked accounts for {}", userId);
        accounts = als.getAccounts(userId, providers.keySet());
    }
    
}
