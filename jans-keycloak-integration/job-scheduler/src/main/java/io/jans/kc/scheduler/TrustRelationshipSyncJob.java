package io.jans.kc.scheduler;

import io.jans.kc.scheduler.job.ExecutionContext;
import io.jans.kc.scheduler.job.RecurringJob;
import io.jans.saml.metadata.model.EntityDescriptor;
import io.jans.saml.metadata.model.SAMLMetadata;
import io.jans.kc.api.admin.client.KeycloakApi;
import io.jans.kc.api.admin.client.model.ManagedSamlClient;
import io.jans.kc.api.admin.client.model.ProtocolMapper;
import io.jans.kc.api.admin.client.model.AuthenticationFlow;
import io.jans.kc.api.config.client.JansConfigApi;
import io.jans.kc.api.config.client.model.JansAttributeRepresentation;
import io.jans.kc.api.config.client.model.JansTrustRelationship;
import io.jans.kc.scheduler.App;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class TrustRelationshipSyncJob extends RecurringJob {
    
    private static final Logger log = LoggerFactory.getLogger(TrustRelationshipSyncJob.class);

    private final JansConfigApi jansConfigApi;
    private final KeycloakApi keycloakApi;
    private final String realm;
    private AuthenticationFlow authnBrowserFlow;
    private final String samlUserAttributeMapperId;

    public TrustRelationshipSyncJob() {

        this.jansConfigApi = App.jansConfigApi();
        this.keycloakApi = App.keycloakApi();
        this.realm = App.configuration().keycloakResourcesRealm();
        this.samlUserAttributeMapperId = App.configuration().keycloakResourcesSamlUserAttributeMapper();
        this.authnBrowserFlow = keycloakApi.getAuthenticationFlowFromAlias(realm,App.configuration().keycloakResourcesBrowserFlowAlias());
    }
    
    @Override
    public void run(ExecutionContext context) {
        
        performSyncTasks();
    }

    
    private void performSyncTasks() {

        try {
            log.info("Performing Saml client housekeeping");
            performSamlClientsHousekeeping();
            log.info("Saml client housekeeping complete");

            log.info("Creating new managed saml clients");
            createNewManagedSamlClients();
            log.info("Creating new managed saml clients complete");
            
            log.info("Updating existing managed saml clients");
            updateExistingManagedSamlClients();
            log.info("Updating existing managed saml clients complete");
        }catch(Exception e) {
            log.error("Error running tr sync job",e);
        }
    }

    private void performSamlClientsHousekeeping() {

        deleteUnmanagedSamlClients();
    }

    private void deleteUnmanagedSamlClients() {

        log.debug("Deleting unmanaged SAML clients");
        List<ManagedSamlClient> managedsamlclients = keycloakApi.findAllManagedSamlClients(realm);
        if(managedsamlclients.isEmpty()) {
            log.debug("No previously managed SAML clients found in keycloak.");
            return;
        }else {
            log.debug("Previously managed SAML clients found in keycloak. Count: {}",managedsamlclients.size());
        }

        managedsamlclients.forEach((c) -> {
           if(!jansConfigApi.trustRelationshipExists(c.externalRef())) {
               log.debug("Deleting previously managed SAML client with id: {}",c.keycloakId());
               keycloakApi.deleteManagedSamlClient(realm,c);
           }
        });
    }

    private void createNewManagedSamlClients() {
        
        if(this.authnBrowserFlow == null) {
            log.warn("Misconfigured browser authentication flow, skipping creation of new saml clients");
            return;
        }
        List<JansTrustRelationship> unassociatedtrs = unassociatedJansTrustRelationships();
        if(unassociatedtrs.isEmpty()) {
            log.debug("No unmanaged trust relationships found in Janssen.");
            return;
        }else {
            log.debug("Unmanaged trust relationships found in Janssen. Count: {}",unassociatedtrs.size());
        }
        unassociatedtrs.stream().forEach(this::createNewManagedSamlClient);

    }

    private void createNewManagedSamlClient(final JansTrustRelationship trustrelationship) {
        try {
            log.debug("Creating managed SAML client from Janssen TR with inum {}",trustrelationship.getInum());
            SAMLMetadata metadata = jansConfigApi.getTrustRelationshipSamlMetadata(trustrelationship);
            List<EntityDescriptor> entitydescriptors = metadata.getEntityDescriptors();
            if(!entitydescriptors.isEmpty()) {
                //use first entity descriptor 
                String trinum = trustrelationship.getInum();
                ManagedSamlClient client = keycloakApi.createManagedSamlClient(realm,trinum,authnBrowserFlow,entitydescriptors.get(0));
                //update managed saml client with released attributes 
                List<JansAttributeRepresentation> attrs = jansConfigApi.getTrustRelationshipReleasedAttributes(trustrelationship);
                addReleasedAttributesToManagedSamlClient(client, attrs);
                log.debug("Created managed SAML client with id {} from Janssen TR with inum {}",client.keycloakId(),trinum);
            }
        }catch(Exception e) {
            log.warn("Could not create managed SAML client using tr with inum {}",trustrelationship.getInum());
            log.warn("Resulting exception: ",e);
        }
    }

    private void updateExistingManagedSamlClients() {

        List<JansTrustRelationship> alltr = jansConfigApi.findAllTrustRelationships();
        List<ManagedSamlClient> clients = keycloakApi.findAllManagedSamlClients(realm);
        log.debug("Updating existing managed saml clients. Count: {}",clients.size());
        clients.stream().forEach((c) -> {
            Optional<JansTrustRelationship> tr  = alltr
                .stream()
                .filter((t)->{return c.correspondsToExternalRef(t.getInum());})
                .findFirst();
            if(tr.isPresent()) {
                this.updateExistingSamlClient(c,tr.get());
            }
        });
    }

    private void updateExistingSamlClient(ManagedSamlClient client, JansTrustRelationship trustrelationship) {

        try {
            log.debug("Updating managed SAML client with id {}. Associated trust relationship inum: {}",client.keycloakId(),client.externalRef());
            SAMLMetadata metadata = jansConfigApi.getTrustRelationshipSamlMetadata(trustrelationship);
            List<EntityDescriptor> entitydescriptors = metadata.getEntityDescriptors();
            if(!entitydescriptors.isEmpty()) {

                keycloakApi.updateManagedSamlClient(realm, client, entitydescriptors.get(0));
                List<JansAttributeRepresentation> releasedattributes = jansConfigApi.getTrustRelationshipReleasedAttributes(trustrelationship);
                List<ProtocolMapper> mappers = keycloakApi.getManagedSamlClientProtocolMappers(realm, client);

                //delete attributes to stop releasing
                mappers.forEach((m)-> {
                    String inum = inumFromProtocolMapperName(m.getName());
                    if(!releasedattributes.stream().anyMatch((r)-> { return inum.equals(r.getInum());}))
                    {
                        log.debug("Removing attribute {} for managed saml client {} because it's no more part of the released attributes",
                        m.getName(),client.clientId());
                        deleteProtolMapperFromManagedClient(client,m);
                    }
                });
                //create new attributes to release
                List<JansAttributeRepresentation> newattributes = releasedattributes
                    .stream().filter((r)-> {
                        return !mappers.stream().anyMatch((m)-> {
                            String inum = inumFromProtocolMapperName(m.getName());
                            return inum.equals(r.getInum());
                        });
                    }).toList();
                addReleasedAttributesToManagedSamlClient(client, newattributes);

                //update existing attributes
                mappers.forEach((m)-> {
                    String inum = inumFromProtocolMapperName(m.getName());
                    Optional<JansAttributeRepresentation> attr = releasedattributes.stream().filter((r)->{
                        return inum.equals(r.getInum());
                    }).findFirst();
                    if(attr.isPresent()) {
                        updateManagedSamlClientProtocolMapper(client,m,attr.get());
                    }
                });
            }
        }catch(Exception e) {
            log.warn("Could not update managed SAML client with id {}",client.keycloakId());
            log.warn("Resulting exception: ",e);
        }
    }

    private List<JansTrustRelationship> unassociatedJansTrustRelationships() {

        List<JansTrustRelationship> alltr = jansConfigApi.findAllTrustRelationships();
        List<ManagedSamlClient> clients = keycloakApi.findAllManagedSamlClients(realm);
        return alltr.stream().filter((t)-> {
            return clients.stream().noneMatch((c) -> {return c.externalRef().equals(t.getInum());});
        }).toList();
    }

    private void addReleasedAttributesToManagedSamlClient(ManagedSamlClient client, List<JansAttributeRepresentation> releasedattributes) {

        List<ProtocolMapper> protmappers = releasedattributes.stream().map((r)-> {
            log.debug("Preparing to add released attribute {} to managed saml client with clientId {}",r.getName(),client.clientId());
            return ProtocolMapper
                   .samlUserAttributeMapper(samlUserAttributeMapperId)
                   .name(generateKeycloakUniqueProtocolMapperName(r))
                   .jansAttributeName(r.getName())
                   .build();
        }).toList();

        keycloakApi.addProtocolMappersToManagedSamlClient(realm, client, protmappers);
    }

    private void updateManagedSamlClientProtocolMapper(ManagedSamlClient client, ProtocolMapper mapper, JansAttributeRepresentation releasedattribute) {

        log.debug("Updating managed client released attribute. Client id: {} / Attribute name: {}",client.clientId(),releasedattribute.getName());
        ProtocolMapper newmapper = ProtocolMapper
            .samlUserAttributeMapper(mapper)
            .jansAttributeName(releasedattribute.getName())
            .build();
        keycloakApi.updateManagedSamlClientProtocolMapper(realm, client,newmapper);
    }

    private void deleteProtolMapperFromManagedClient(ManagedSamlClient client,ProtocolMapper mapper) {

        log.debug("Deleting released attribute from managed client. Client id: {} / Attribute name: {}",client.clientId(),mapper.getName());
        keycloakApi.deleteManagedSamlClientProtocolMapper(realm,client,mapper);
    }

    private final String generateKeycloakUniqueProtocolMapperName(JansAttributeRepresentation rep) {

        return String.format("%s:%s",rep.getInum(),rep.getName());
    }

    private final String inumFromProtocolMapperName(String name) {

        int idx = name.indexOf(":");
        if(idx!= -1) {
            return name.substring(0,idx);
        }
        return "";
    }

}
