package io.jans.configapi.service;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.inject.Named;

import io.jans.as.model.configuration.AppConfiguration;

@ApplicationScoped
@Named("attributeService")
public class AttributeService  extends io.jans.as.common.service.AttributeService {

    protected boolean isUseLocalCache() {
        return false;
    }
}
