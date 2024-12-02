package io.jans.configapi.service.auth;

import com.fasterxml.jackson.databind.JsonNode;

import io.jans.configapi.model.configuration.ApiAppConfiguration;
import io.jans.configapi.security.client.AuthClientFactory;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.slf4j.Logger;

@ApplicationScoped
public class AgamaRepoService {

    @Inject
    private Logger logger;

    @Inject
    private ApiAppConfiguration appConfiguration;

    @Inject
    AuthClientFactory authClientFactory;

    public JsonNode getAllAgamaRepositories() {
        return AuthClientFactory.getAllAgamaRepositories(getAgamaFormatedRepositoryURL());
    }

    public JsonNode getAgamaProjectLatestRepository(String projectName) {
        return AuthClientFactory.getAgamaProjectLatestRelease(getAgamaProjectFormatedRepositoryURL(projectName));
    }

    public String getAgamaFormatedRepositoryURL() {
        return String.format(getAgamaRepositoryURL(), getAgamaProjectPrefix());
    }

    public String getAgamaProjectFormatedRepositoryURL(String projectName) {
        return String.format(getAgamaLatestRepositoryURL(), projectName);
    }

    public String getAgamaProjectPrefix() {
        return this.appConfiguration.getAgamaProjectPrefix();
    }

    public String getAgamaRepositoryURL() {
        return this.appConfiguration.getAgamaRepositoryURL();
    }

    public String getAgamaLatestRepositoryURL() {
        return this.appConfiguration.getAgamaLatestRepositoryURL();
    }

}