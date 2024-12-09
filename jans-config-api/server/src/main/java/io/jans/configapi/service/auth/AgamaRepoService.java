package io.jans.configapi.service.auth;

import com.fasterxml.jackson.databind.JsonNode;

import io.jans.configapi.service.status.StatusCheckerTimer;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.slf4j.Logger;

@ApplicationScoped
public class AgamaRepoService {

    @Inject
    private Logger logger;

    @Inject
    private StatusCheckerTimer statusCheckerTimer;

    public JsonNode getAllAgamaRepositories() {
        return statusCheckerTimer.getAllAgamaRepositories();
    }

    public JsonNode getAgamaProjectLatestRepository(String repositoryName) {
        return statusCheckerTimer.getAllAgamaRepositories();
    }
    
}