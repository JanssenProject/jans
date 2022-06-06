package io.jans.ca.server.service.auth;

import io.jans.as.common.service.AttributeService;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class AttributeServiceImpl extends AttributeService {


    @Override
    protected boolean isUseLocalCache() {
        return false;
    }
}