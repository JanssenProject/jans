package io.jans.as.server.service.external;

import io.jans.model.custom.script.CustomScriptType;
import io.jans.service.custom.script.ExternalScriptService;
import jakarta.ejb.DependsOn;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Named;

/**
 * @author Yuriy Z
 */
@ApplicationScoped
@DependsOn("appInitializer")
@Named
public class ExternalTokenExchangeService extends ExternalScriptService {

    public ExternalTokenExchangeService() {
        super(CustomScriptType.TOKEN_EXCHANGE);
    }
}
