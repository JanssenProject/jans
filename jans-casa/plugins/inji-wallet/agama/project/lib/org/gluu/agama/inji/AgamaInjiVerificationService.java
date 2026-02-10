package org.gluu.agama.inji;

import java.util.HashMap;
import java.util.Map;

import org.gluu.agama.inji.AgamaInjiVerificationServiceImpl;

public abstract class AgamaInjiVerificationService{

    public abstract Map<String, Object> createVpVerificationRequest();

    public abstract String buildInjiWebAuthorizationUrl(String requestId, String transactionId);

    public abstract Map<String, Object> verifyInjiAppResult(String requestId, String transactionId);

    public abstract Map<String, String> extractUserInfoFromVC();

    public abstract Map<String, String> checkUserExists(String email);

    public abstract Map<String, String> onboardUser(Map<String, String> userInfo, String password);

    public static AgamaInjiVerificationService getInstance(HashMap config){
        return AgamaInjiVerificationServiceImpl.getInstance(config);
    }
}
