package io.jans.as.server.register.ws.rs;

import io.jans.as.model.jwt.Jwt;
import io.jans.as.model.ssa.SsaValidationConfig;
import io.jans.as.model.ssa.SsaValidationType;

/**
 * @author Yuriy Zabrovarnyy
 */
public class SsaValidationConfigContext {

    private final Jwt jwt;
    private final SsaValidationType type;

    private SsaValidationConfig successfulConfig;

    public SsaValidationConfigContext(Jwt jwt, SsaValidationType type) {
        this.jwt = jwt;
        this.type = type;
    }

    public Jwt getJwt() {
        return jwt;
    }

    public SsaValidationType getType() {
        return type;
    }

    public SsaValidationConfig getSuccessfulConfig() {
        return successfulConfig;
    }

    public void setSuccessfulConfig(SsaValidationConfig successfulConfig) {
        this.successfulConfig = successfulConfig;
    }
}
