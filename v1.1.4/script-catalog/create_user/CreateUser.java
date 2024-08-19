import io.jans.as.common.model.common.User;
import io.jans.as.server.service.RequestParameterService;
import io.jans.as.server.service.external.context.ExternalScriptContext;
import io.jans.model.SimpleCustomProperty;
import io.jans.model.custom.script.model.CustomScript;
import io.jans.model.custom.script.type.createuser.CreateUserType;
import io.jans.service.cdi.util.CdiUtil;
import io.jans.service.custom.script.CustomScriptManager;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Sample custom script for user creation on prompt=create
 *
 * @author Yuriy Z
 */
public class CreateUser implements CreateUserType {

    private static final Logger log = LoggerFactory.getLogger(CreateUser.class);
    private static final Logger scriptLogger = LoggerFactory.getLogger(CustomScriptManager.class);

    /**
     * Returns custom page for user creation
     *
     * @param context ExternalScriptContext, see https://github.com/JanssenProject/jans/blob/main/jans-auth-server/server/src/main/java/io/jans/as/server/service/external/context/ExternalScriptContext.java
     * @return custom page
     */
    @Override
    public String getCreateUserPage(Object context) {
        return "/customCreateUser";
    }

    /**
     * Preparetion of user create action (called right before page is shown)
     *
     * @param context ExternalScriptContext, see https://github.com/JanssenProject/jans/blob/main/jans-auth-server/server/src/main/java/io/jans/as/server/service/external/context/ExternalScriptContext.java
     * @return whether preparetion is successful or not. If "false" is returned then error is shown.
     */
    @Override
    public boolean prepare(Object context) {
        // make some preparations
        return true;
    }

    /**
     * Method is called before user creation (persistence to DB).
     * "context.getExecutionContext().getUser()" allows to access and modify user object that
     * will be persisted.
     *
     * @param context ExternalScriptContext, see https://github.com/JanssenProject/jans/blob/main/jans-auth-server/server/src/main/java/io/jans/as/server/service/external/context/ExternalScriptContext.java
     * @return whether user is created successfully. If "false" is returned, user creation is interrupted.
     */
    @Override
    public boolean createUser(Object context) {
        ExternalScriptContext scriptContext = (ExternalScriptContext) context;
        final User user = scriptContext.getExecutionContext().getUser();
        final Map<String, String[]> parameterMap = scriptContext.getExecutionContext().getHttpRequest().getParameterMap();
        return true;
    }

    /**
     * Returns post authorization url. After user is created, user-agent is redirected to Authorization Endpoint. Here it can be customized/modified.
     *
     * @param context ExternalScriptContext, see https://github.com/JanssenProject/jans/blob/main/jans-auth-server/server/src/main/java/io/jans/as/server/service/external/context/ExternalScriptContext.java
     * @return authorization url
     */
    @Override
    public String buildPostAuthorizeUrl(Object context) {
        ExternalScriptContext scriptContext = (ExternalScriptContext) context;
        final User user = scriptContext.getExecutionContext().getUser();
        final HttpServletRequest httpRequest = scriptContext.getExecutionContext().getHttpRequest();
        final Map<String, String[]> requestParameters = httpRequest.getParameterMap();

        final Map<String, String> parameters = new HashMap<>();
        // construct/fill parameters here

        try {
            RequestParameterService requestParameterService = CdiUtil.bean(RequestParameterService.class);
            return httpRequest.getContextPath() + "/restv1/authorize?" + requestParameterService.parametersAsString(parameters);
        } catch (Exception e) {
            scriptLogger.error("Failed to build post authorization url.", e);
            return null;
        }
    }

    @Override
    public boolean init(Map<String, SimpleCustomProperty> configurationAttributes) {
        scriptLogger.info("Initialized CreateUser Java custom script.");
        return false;
    }

    @Override
    public boolean init(CustomScript customScript, Map<String, SimpleCustomProperty> configurationAttributes) {
        scriptLogger.info("Initialized CreateUser Java custom script.");
        return false;
    }

    @Override
    public boolean destroy(Map<String, SimpleCustomProperty> configurationAttributes) {
        scriptLogger.info("Destroyed CreateUser Java custom script.");
        return false;
    }

    @Override
    public int getApiVersion() {
        return 11;
    }
}
