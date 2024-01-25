package io.jans.kc.scheduler;

import io.jans.kc.scheduler.job.ExecutionContext;
import io.jans.kc.scheduler.job.RecurringJob;
import io.jans.kc.api.admin.client.KeycloakApi;
import io.jans.kc.api.admin.client.model.ManagedSamlClient;
import io.jans.kc.api.config.client.JansConfigApi;
import io.jans.kc.api.config.client.model.JansTrustRelationship;
import io.jans.kc.scheduler.App;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class TrustRelationshipSyncJob extends RecurringJob {
    
    private static final Logger log = LoggerFactory.getLogger(TrustRelationshipSyncJob.class);

    private JansConfigApi jansConfigApi;
    private KeycloakApi keycloakApi;
    private String realm;

    public TrustRelationshipSyncJob() {

        this.jansConfigApi = App.jansConfigApi();
        this.keycloakApi = App.keycloakApi();
        this.realm = App.configuration().keycloakAdminRealm();
    }
    
    @Override
    public void run(ExecutionContext context) {
        
        try {
            log.debug("Performing Saml client housekeeping");
            performSamlClientsHousekeeping();
            log.debug("Saml client housekeeping complete");

            log.debug("Synchronizing new trustrelationships");
            syncNewTrustRelationships();
            log.debug("New trustrelationships sync complete");
        }catch(Exception e) {
            log.error("Error running tr sync job",e);
        }
    }

    private void performSamlClientsHousekeeping() {

        log.debug("Saml housekeeping start for realm -- {}",realm);
        List<ManagedSamlClient> samlclients = keycloakApi.findAllManagedSamlClients(realm);
        for(ManagedSamlClient samlclient : samlclients) {
            String jans_tr_inum = samlclient.trustRelationshipInum();
            log.debug("Housekeeping attempt for SAML client -- {}",samlclient.keycloakId());
            if(!jansConfigApi.trustRelationshipExists(jans_tr_inum)) {
                log.debug("Deleting SAML client -- {}",samlclient.keycloakId());
                keycloakApi.deleteManagedSamlClient(realm,samlclient);
            }
        }
        log.debug("Saml housekeeping exit for realm -- {}",realm);
    }

    private void syncNewTrustRelationships() {

        List<JansTrustRelationship> unmanagedtrs = unmanagedTrustRelationships();
        unmanagedtrs.forEach(new CreateSamlClientFromTrustRelationship());
        
    }

    private void syncExistingTrustRelationships() {

    }

    private List<JansTrustRelationship> unmanagedTrustRelationships() {

        List<ManagedSamlClient> samlclients = keycloakApi.findAllManagedSamlClients(realm);
        return filteredTrustRelationships(new HasAnAssociatedSamlClient(samlclients).negate());
    }

    private List<JansTrustRelationship> managedTrustRelationships() {

        List<ManagedSamlClient> samlclients = keycloakApi.findAllManagedSamlClients(realm);
        return filteredTrustRelationships(new HasAnAssociatedSamlClient(samlclients));
    }

    private List<JansTrustRelationship> filteredTrustRelationships(final Predicate<JansTrustRelationship> filter) {

        List<JansTrustRelationship> alltr = jansConfigApi.findAllTrustRelationships();
        return alltr
            .stream()
            .filter(filter)
            .collect(Collectors.toList());
    }

    private class HasAnAssociatedSamlClient implements Predicate<JansTrustRelationship> {

        private List<ManagedSamlClient> samlclients;

        public HasAnAssociatedSamlClient(List<ManagedSamlClient>  samlclients) {

            this.samlclients = samlclients;
        }


        @Override
        public boolean test(JansTrustRelationship t) {

            for(ManagedSamlClient c : samlclients) {
                if(c.trustRelationshipInum().equalsIgnoreCase(t.getInum())) {
                    return true;
                }
            }
            return false;
        }

    }

    private class CreateSamlClientFromTrustRelationship implements Consumer<JansTrustRelationship> {

        @Override
        public void accept(JansTrustRelationship tr) {

        }

        
    }

    private class UpdateSamlClientUsingTrustRelationship implements Consumer<JansTrustRelationship> {

        @Override
        public void accept(JansTrustRelationship tr) {

        }
    }
}
