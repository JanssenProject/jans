package io.jans.casa.plugins.strongauthn.vm;

import io.jans.casa.plugins.strongauthn.conf.EnforcementPolicy;
import io.jans.casa.plugins.strongauthn.service.StrongAuthSettingsService;
import org.zkoss.bind.annotation.Init;

import java.util.List;
import java.util.stream.Collectors;

import static io.jans.casa.plugins.strongauthn.conf.EnforcementPolicy.CUSTOM;

public class FragmentViewModel {

    private boolean customPolicy;
    private List<String> policies;

    public boolean isCustomPolicy() {
        return customPolicy;
    }

    public List<String> getPolicies() {
        return policies;
    }

    @Init
    public void init() {
        List<EnforcementPolicy> policies2FA = StrongAuthSettingsService.instance().getSettingsHandler().getSettings().getEnforcement2FA();
        customPolicy = policies2FA.contains(CUSTOM);
        if (!customPolicy) {
            policies = policies2FA.stream().map(EnforcementPolicy::toString).collect(Collectors.toList());
        }
    }

}
