package org.xdi.oxauth.uma.authorization;

import org.xdi.model.custom.script.conf.CustomScriptConfiguration;
import org.xdi.oxauth.model.uma.persistence.UmaPermission;
import org.xdi.oxauth.model.uma.persistence.UmaScopeDescription;
import org.xdi.oxauth.service.AttributeService;
import org.xdi.oxauth.uma.service.UmaResourceService;

import java.util.List;
import java.util.Map;

/**
 * @author yuriyz on 06/06/2017.
 */
public class UmaAuthorizationContextBuilder {

    private AttributeService attributeService;
    private UmaResourceService resourceService;
    private List<UmaPermission> permissions;
    private Map<UmaScopeDescription, Boolean> scopes;

    public UmaAuthorizationContextBuilder(AttributeService attributeService, UmaResourceService resourceService,
                                          List<UmaPermission> permissions, Map<UmaScopeDescription, Boolean> scopes) {
        this.attributeService = attributeService;
        this.resourceService = resourceService;
        this.permissions = permissions;
        this.scopes = scopes;
    }

    public UmaAuthorizationContext build(CustomScriptConfiguration script, ) {
        return new UmaAuthorizationContext(attributeService, );
    }
}
